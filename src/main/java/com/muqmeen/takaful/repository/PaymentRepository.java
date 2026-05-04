package com.muqmeen.takaful.repository;

import com.muqmeen.takaful.domain.Customer;
import com.muqmeen.takaful.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Payment findByBillCode(String billCode);

    Payment findByExternalReferenceNo(String externalReferenceNo);

    List<Payment> findAllByCustomerOrderByCreatedAtDesc(Customer customer);
}
