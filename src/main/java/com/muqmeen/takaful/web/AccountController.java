package com.muqmeen.takaful.web;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.Lead;
import com.muqmeen.takaful.service.CustomerService;
import com.muqmeen.takaful.service.TakafulService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class AccountController {

    private final CustomerService customerService;
    private final TakafulService takafulService;

    public AccountController(CustomerService customerService, TakafulService takafulService) {
        this.customerService = customerService;
        this.takafulService = takafulService;
    }

    @GetMapping("/account")
    public String account(Authentication authentication, Model model) {
        Customer customer = customerService.currentCustomer(authentication)
                .orElseThrow(() -> new IllegalStateException("Authenticated customer not found"));
        List<Lead> leads = takafulService.getLeadsForCustomer(customer);

        long pending = leads.stream().filter(lead -> "NEW".equals(lead.getLeadStatus())).count();
        long contacted = leads.stream().filter(lead -> "CONTACTED".equals(lead.getLeadStatus())).count();
        long closed = leads.stream().filter(lead -> "CLOSED".equals(lead.getLeadStatus())).count();
        BigDecimal paidTips = leads.stream()
                .filter(lead -> "PAID".equals(lead.getPaymentStatus()))
                .map(Lead::getTipAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("customer", customer);
        model.addAttribute("leads", leads);
        model.addAttribute("totalConsultations", leads.size());
        model.addAttribute("pendingConsultations", pending);
        model.addAttribute("contactedConsultations", contacted);
        model.addAttribute("closedConsultations", closed);
        model.addAttribute("paidTips", paidTips);
        return "account";
    }
}
