package com.muqmeen.takaful.repository;

import com.muqmeen.takaful.domain.Lead;
import com.muqmeen.takaful.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    List<Lead> findAllByOrderByCreatedAtDesc();

    List<Lead> findAllByCustomerOrderByCreatedAtDesc(Customer customer);

    Optional<Lead> findByIdAndCustomer(Long id, Customer customer);

    Lead findByBillCode(String billCode);
}
