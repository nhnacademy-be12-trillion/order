package com.nhnacademy.payment.repository;

import com.nhnacademy.payment.domain.Payment;
import com.nhnacademy.payment.domain.PaymentStatus;
import com.nhnacademy.payment.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    //Optional<Payment> findByOrder_OrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"order"})
    Payment findByOrder_OrderNumber(String orderNumber);

}
