package com.nhnacademy.order.ordersaga.itemrefund.repository;

import com.nhnacademy.order.ordersaga.domain.SagaStatus;
import com.nhnacademy.order.ordersaga.itemrefund.domain.NonMemberOrderItemRefundSaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NonMemberOrderItemRefundSagaRepository extends JpaRepository<NonMemberOrderItemRefundSaga, Long> {
    List<NonMemberOrderItemRefundSaga> findAllByOverallStatusInAndUpdatedAtBefore(List<SagaStatus> sagaStatuses, LocalDateTime updatedAt);
    List<NonMemberOrderItemRefundSaga> findAllByOverallStatusAndBridgedFalseAndUpdatedAtBefore(SagaStatus status, LocalDateTime updatedAt);
}
