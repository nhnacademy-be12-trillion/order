package com.nhnacademy.order.order.repository;

import com.nhnacademy.order.order.domain.OrderSaga;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderSagaRepository extends JpaRepository<OrderSaga, Long> {
}
