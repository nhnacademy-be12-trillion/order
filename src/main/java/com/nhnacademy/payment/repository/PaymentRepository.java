package com.nhnacademy.payment.repository;

import com.nhnacademy.payment.entity.Payment;
import io.github.resilience4j.core.lang.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    //Optional<Payment> findByOrder_OrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"order"})
    Payment findByOrder_OrderNumber(String orderNumber);

    @NonNull
    @EntityGraph(attributePaths = {"order"})
    Optional<Payment> findById(@NonNull Long id);

    @NonNull
    @EntityGraph(attributePaths = {"order"})
    Page<Payment> findAll(@NonNull Pageable pageable);

}
