package com.muqmeen.takaful.web;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.Lead;
import com.muqmeen.takaful.domain.Payment;
import com.muqmeen.takaful.service.CustomerService;
import com.muqmeen.takaful.service.PaymentService;
import com.muqmeen.takaful.service.ProductService;
import com.muqmeen.takaful.service.TakafulService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@Controller
public class WebController {

    private final TakafulService takafulService;
    private final ProductService productService;
    private final CustomerService customerService;
    private final PaymentService paymentService;

    public WebController(TakafulService takafulService,
                         ProductService productService,
                         CustomerService customerService,
                         PaymentService paymentService) {
        this.takafulService = takafulService;
        this.productService = productService;
        this.customerService = customerService;
        this.paymentService = paymentService;
    }

    @GetMapping("/")
    public String landingPage(@RequestParam(value = "productId", required = false) Long productId,
                              Authentication authentication,
                              Model model) {
        Optional<Customer> customer = customerService.currentCustomer(authentication);
        if (!model.containsAttribute("lead")) {
            Lead lead = new Lead();
            customer.ifPresent(c -> {
                lead.setFullName(c.getFullName());
                lead.setPhoneNumber(c.getPhoneNumber());
            });
            model.addAttribute("lead", lead);
        }
        model.addAttribute("products", productService.listActiveForLanding());
        model.addAttribute("customerSignedIn", customer.isPresent());
        customer.ifPresent(c -> model.addAttribute("currentCustomer", c));
        if (customer.isPresent() && productId != null) {
            productService.findActiveById(productId)
                    .ifPresent(product -> model.addAttribute("selectedProductName", product.getName()));
        }
        return "index";
    }

    @PostMapping("/submit-lead")
    public String submitLead(@Valid @ModelAttribute("lead") Lead lead,
                             BindingResult bindingResult,
                             Authentication authentication,
                             Model model) {
        Customer customer = customerService.currentCustomer(authentication)
                .orElseThrow(() -> new IllegalStateException("Authenticated customer not found"));
        if (bindingResult.hasErrors()) {
            model.addAttribute("formError", true);
            model.addAttribute("products", productService.listActiveForLanding());
            model.addAttribute("customerSignedIn", true);
            model.addAttribute("currentCustomer", customer);
            return "index";
        }

        Lead savedLead = takafulService.processNewLead(lead, customer);
        PaymentService.PaymentStart paymentStart = paymentService.prepareTipPayment(savedLead);
        return "redirect:" + paymentStart.redirectUrl();
    }

    @GetMapping("/payment/mock/{billCode}")
    public String mockPaymentPage(@PathVariable String billCode, Model model) {
        model.addAttribute("billCode", billCode);
        return "payment_mock";
    }

    @GetMapping("/payment/callback")
    public String paymentCallback(@RequestParam("billcode") String billCode,
                                  @RequestParam("status_id") String statusId) {
        Payment payment = paymentService.updateMockStatus(billCode, statusId);
        return Optional.of(payment.getLead())
                .map(lead -> "redirect:/success?leadId=" + lead.getId())
                .orElse("redirect:/success");
    }

    @PostMapping("/payment/callback")
    @ResponseBody
    public ResponseEntity<String> toyyibPayCallback(@RequestParam Map<String, String> params) {
        return paymentService.processCallback(params)
                ? ResponseEntity.ok("OK")
                : ResponseEntity.badRequest().body("INVALID");
    }

    @GetMapping("/payment/return")
    public String paymentReturn(@RequestParam(value = "billcode", required = false) String billCode,
                                @RequestParam(value = "status_id", required = false) String statusId,
                                Model model) {
        model.addAttribute("billCode", billCode);
        model.addAttribute("statusId", statusId);
        if (billCode != null) {
            paymentService.findByBillCode(billCode)
                    .ifPresent(payment -> model.addAttribute("lead", payment.getLead()));
        }
        return "payment_return";
    }

    @GetMapping("/success")
    public String successPage(@RequestParam(value = "leadId", required = false) Long leadId,
                              Authentication authentication,
                              Model model) {
        if (leadId != null) {
            Optional<Customer> customer = customerService.currentCustomer(authentication);
            takafulService.findLead(leadId)
                    .filter(lead -> lead.getCustomer() == null
                            || customer.map(c -> lead.getCustomer().getId().equals(c.getId())).orElse(false))
                    .ifPresent(lead -> model.addAttribute("lead", lead));
        }
        return "success";
    }

    @GetMapping("/admin")
    public String adminEntry() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        List<Lead> leads = takafulService.getAllLeadsForAdmin();
        model.addAttribute("leads", leads);

        BigDecimal totalTips = leads.stream()
                .filter(l -> "PAID".equals(l.getPaymentStatus()))
                .map(Lead::getTipAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalTips", totalTips);
        return "admin/dashboard";
    }

    @PostMapping("/admin/lead/{id}/status")
    @ResponseBody
    public String updateStatus(@PathVariable Long id, @RequestParam String status) {
        takafulService.updateLeadStatus(id, status);
        return "OK";
    }
}
