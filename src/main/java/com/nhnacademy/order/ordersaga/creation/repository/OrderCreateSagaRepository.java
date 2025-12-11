package com.nhnacademy.order.ordersaga.creation.repository;

import com.nhnacademy.order.ordersaga.creation.domain.OrderCreateSaga;
import com.nhnacademy.order.ordersaga.domain.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderCreateSagaRepository extends JpaRepository<OrderCreateSaga, Long> {
    List<OrderCreateSaga> findAllByOverallStatusInAndUpdatedAtBefore(List<SagaStatus> sagaStatuses, LocalDateTime updatedAt);
    List<OrderCreateSaga> findAllByOverallStatusAndBridgedFalseAndUpdatedAtBefore(SagaStatus sagaStatus, LocalDateTime updatedAt);
}
