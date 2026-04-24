package com.muqmeen.takaful;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Main application class for Smart Takaful & Consultation System.
 * Architecture: Layered Architecture (Entity -> Repository -> Service -> Controller)
 */
@SpringBootApplication
public class SmartTakafulApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartTakafulApplication.class, args);
    }

    /* =========================================================================================
     * 1. DOMAIN LAYER (ENTITIES)
     * ========================================================================================= */
     
    @Entity
    @Table(name = "leads")
    public static class Lead {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @NotBlank(message = "Name is required")
        private String fullName;

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{10,13}$", message = "Invalid phone number format")
        private String phoneNumber;

        @NotBlank(message = "Product interest is required")
        private String productType;

        @NotBlank(message = "Consultation mode is required")
        private String consultationMode;

        private BigDecimal tipAmount;
        
        // ToyyibPay transaction tracking
        private String billCode;
        private String paymentStatus; // PENDING, PAID, SKIPPED

        private String leadStatus; // NEW, CONTACTED, CLOSED
        
        @Column(updatable = false)
        private LocalDateTime createdAt;

        @PrePersist
        protected void onCreate() {
            this.createdAt = LocalDateTime.now();
            if (this.leadStatus == null) this.leadStatus = "NEW";
            if (this.paymentStatus == null) this.paymentStatus = "PENDING";
        }

        // Standard Getters and Setters (Omitted for brevity in prototype, assume present)
        public Long getId() { return id; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getProductType() { return productType; }
        public void setProductType(String productType) { this.productType = productType; }
        public String getConsultationMode() { return consultationMode; }
        public void setConsultationMode(String consultationMode) { this.consultationMode = consultationMode; }
        public BigDecimal getTipAmount() { return tipAmount; }
        public void setTipAmount(BigDecimal tipAmount) { this.tipAmount = tipAmount; }
        public String getLeadStatus() { return leadStatus; }
        public void setLeadStatus(String leadStatus) { this.leadStatus = leadStatus; }
        public String getBillCode() { return billCode; }
        public void setBillCode(String billCode) { this.billCode = billCode; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    /* =========================================================================================
     * 2. DATA ACCESS LAYER (REPOSITORIES)
     * ========================================================================================= */

    public interface LeadRepository extends JpaRepository<Lead, Long> {
        List<Lead> findAllByOrderByCreatedAtDesc();
        Lead findByBillCode(String billCode);
    }

    /* =========================================================================================
     * 3. SERVICE LAYER (BUSINESS LOGIC)
     * ========================================================================================= */

    @Service
    public static class TakafulService {
        private final LeadRepository leadRepository;

        public TakafulService(LeadRepository leadRepository) {
            this.leadRepository = leadRepository;
        }

        /**
         * Processes a new lead submission. If a tip is provided, generates a mock ToyyibPay BillCode.
         */
        public Lead processNewLead(Lead lead) {
            if (lead.getTipAmount() != null && lead.getTipAmount().compareTo(BigDecimal.ZERO) > 0) {
                // Mock ToyyibPay Integration: Generate external bill code
                String generatedBillCode = "MGM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                lead.setBillCode(generatedBillCode);
                lead.setPaymentStatus("PENDING");
            } else {
                lead.setPaymentStatus("SKIPPED"); // No payment required
            }
            return leadRepository.save(lead);
        }

        public List<Lead> getAllLeadsForAdmin() {
            return leadRepository.findAllByOrderByCreatedAtDesc();
        }

        public void updateLeadStatus(Long id, String status) {
            Lead lead = leadRepository.findById(id).orElseThrow(() -> new RuntimeException("Lead not found"));
            lead.setLeadStatus(status);
            leadRepository.save(lead);
        }
        
        public void updatePaymentStatus(String billCode, String status) {
            Lead lead = leadRepository.findByBillCode(billCode);
            if(lead != null) {
                lead.setPaymentStatus(status);
                leadRepository.save(lead);
            }
        }
    }

    /* =========================================================================================
     * 4. PRESENTATION LAYER (CONTROLLERS)
     * ========================================================================================= */

    @Controller
    public static class WebController {
        private final TakafulService takafulService;

        public WebController(TakafulService takafulService) {
            this.takafulService = takafulService;
        }

        // Public Landing Page (Mapped to Thymeleaf template 'index.html')
        @GetMapping("/")
        public String landingPage(Model model) {
            model.addAttribute("lead", new Lead());
            return "index"; // Thymeleaf resolves to src/main/resources/templates/index.html
        }

        // Form Submission Endpoint
        @PostMapping("/submit-lead")
        public String submitLead(@ModelAttribute Lead lead) {
            Lead savedLead = takafulService.processNewLead(lead);
            
            // Redirect logic based on Tip amount
            if (savedLead.getBillCode() != null) {
                // Redirect to actual ToyyibPay gateway in production
                // return "redirect:https://dev.toyyibpay.com/" + savedLead.getBillCode();
                return "redirect:/payment/mock/" + savedLead.getBillCode();
            }
            return "redirect:/success";
        }

        // Mock Payment Gateway Page
        @GetMapping("/payment/mock/{billCode}")
        public String mockPaymentPage(@PathVariable String billCode, Model model) {
            model.addAttribute("billCode", billCode);
            return "payment_mock"; 
        }

        // Payment Callback (Webhook from ToyyibPay)
        @GetMapping("/payment/callback")
        public String paymentCallback(@RequestParam("billcode") String billCode, @RequestParam("status_id") String statusId) {
            // Status 1 = Success in ToyyibPay
            if ("1".equals(statusId)) {
                takafulService.updatePaymentStatus(billCode, "PAID");
            }
            return "redirect:/success";
        }

        @GetMapping("/success")
        public String successPage() {
            return "success";
        }

        // Admin Dashboard (Protected by Spring Security in full app)
        @GetMapping("/admin/dashboard")
        public String adminDashboard(Model model) {
            List<Lead> leads = takafulService.getAllLeadsForAdmin();
            model.addAttribute("leads", leads);
            
            // Calculate Total Tips
            BigDecimal totalTips = leads.stream()
                .filter(l -> "PAID".equals(l.paymentStatus))
                .map(Lead::getTipAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
            model.addAttribute("totalTips", totalTips);
            return "admin/dashboard";
        }
        
        // Admin REST endpoint for AJAX status updates
        @PostMapping("/admin/lead/{id}/status")
        @ResponseBody
        public String updateStatus(@PathVariable Long id, @RequestParam String status) {
            takafulService.updateLeadStatus(id, status);
            return "OK";
        }
    }
}