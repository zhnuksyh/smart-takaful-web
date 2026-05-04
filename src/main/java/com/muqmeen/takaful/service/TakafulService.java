package com.muqmeen.takaful.service;

import com.muqmeen.takaful.domain.Lead;
import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.repository.LeadRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class TakafulService {

    private final LeadRepository leadRepository;

    public TakafulService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    public Lead processNewLead(Lead lead, Customer customer) {
        lead.setPhoneNumber(normalizePhoneNumber(lead.getPhoneNumber()));
        lead.setCustomer(customer);

        if (lead.getTipAmount() != null && lead.getTipAmount().compareTo(BigDecimal.ZERO) > 0) {
            lead.setPaymentStatus("PENDING");
        } else {
            lead.setPaymentStatus("SKIPPED");
        }
        return leadRepository.save(lead);
    }

    private String normalizePhoneNumber(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("[^0-9]", "");
    }

    public List<Lead> getAllLeadsForAdmin() {
        return leadRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Lead> getLeadsForCustomer(Customer customer) {
        return leadRepository.findAllByCustomerOrderByCreatedAtDesc(customer);
    }

    public Optional<Lead> findLead(Long id) {
        return leadRepository.findById(id);
    }

    public Optional<Lead> findCustomerLead(Long id, Customer customer) {
        return leadRepository.findByIdAndCustomer(id, customer);
    }

    public Optional<Lead> findLeadByBillCode(String billCode) {
        return Optional.ofNullable(leadRepository.findByBillCode(billCode));
    }

    public void updateLeadStatus(Long id, String status) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead not found"));
        lead.setLeadStatus(status);
        leadRepository.save(lead);
    }

    public void updatePaymentStatus(String billCode, String status) {
        Lead lead = leadRepository.findByBillCode(billCode);
        if (lead != null) {
            lead.setPaymentStatus(status);
            leadRepository.save(lead);
        }
    }
}
