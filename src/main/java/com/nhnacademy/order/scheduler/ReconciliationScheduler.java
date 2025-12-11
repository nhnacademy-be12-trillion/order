package com.nhnacademy.order.scheduler;

import com.nhnacademy.order.order.domain.OrderStatus;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.ordersaga.cancellation.repository.OrderCancelSagaRepository;
import com.nhnacademy.order.ordersaga.creation.repository.OrderCreateSagaRepository;
import com.nhnacademy.order.ordersaga.domain.SagaStatus;
import com.nhnacademy.order.ordersaga.itemrefund.repository.NonMemberOrderItemRefundSagaRepository;
import com.nhnacademy.order.ordersaga.itemrefund.repository.OrderItemRefundSagaRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReconciliationScheduler {
    private final OrderRepository orderRepository;
    private final OrderCreateSagaRepository orderCreateSagaRepository;
    private final OrderCancelSagaRepository orderCancelSagaRepository;
    private final OrderItemRefundSagaRepository orderItemRefundSagaRepository;
    private final ReconciliationService reconciliationService;
    private final NonMemberOrderItemRefundSagaRepository nonMemberOrderItemRefundSagaRepository;

    // 처음에는 불완전한 주문 생성 사가를 다시 처리해 주문을 생성하려 했지만, 스케줄러가 잡은 모든 주문 생성 사가는 보상 처리하는 것이 비즈니스 흐름 상 올바른 것 같음.
    // 주문 생성 -> 주문 생성 완료 -> 결제까지 한 번에 동기적으로 성공해야 함.

    // 동기적으로 잘 생성되고 있는 주문을 스케줄러가 잡게 되는 경우가 발생하지 않도록 수정 시간(updatedAt) 사용

    // 멈춰있거나 완료된 사가를 처리
    @Scheduled(fixedRate = 300000)
    @SchedulerLock(name = "ReconciliationScheduler.reconcileSagaState") // 분산 락 설정 -> 서버가 여러 개여도 하나의 스케줄러만 실행
    public void reconcileSagaState() {
        // 변경 시간(updatedAt)이 1분이 지난 대상만 처리 (수정 가능)
        // @Scheduled가 사용된 메서드는 매개변수 사용 불가능
        LocalDateTime cutOffTime = LocalDateTime.now().minusMinutes(1);

        // 1. 멈춘 사가 복구 / 재시도
        reconcileStuckSagas(cutOffTime);

        // 2. 완료된 사가와 도메인 연결
        bridgeCompletedSagas(cutOffTime);
    }

    // 사가를 진행하다 멈춘 경우를 처리하는 로직
    private void reconcileStuckSagas(LocalDateTime cutOffTime) {
        // 주문 생성 사가 -> 보상 처리
        orderCreateSagaRepository.findAllByOverallStatusInAndUpdatedAtBefore(
                List.of(SagaStatus.PROGRESS, SagaStatus.COMPENSATED),
                cutOffTime
        ).forEach(reconciliationService::processStuckCreateSagaCompensation);

        // 주문 취소 사가 -> 재시도
        orderCancelSagaRepository.findAllByOverallStatusInAndUpdatedAtBefore(
                List.of(SagaStatus.PROGRESS, SagaStatus.FAILED),
                cutOffTime
        ).forEach(reconciliationService::processStuckCancelSagaRetry);

        // 주문 상품 환불 사가 (회원) -> 재시도
        orderItemRefundSagaRepository.findAllByOverallStatusInAndUpdatedAtBefore(
                List.of(SagaStatus.PROGRESS, SagaStatus.FAILED),
                cutOffTime
        ).forEach(reconciliationService::processStuckRefundItemSagaRetry);

        // 주문 상품 환불 사가 (비회원) -> 재시도
        nonMemberOrderItemRefundSagaRepository.findAllByOverallStatusInAndUpdatedAtBefore(
                List.of(SagaStatus.PROGRESS, SagaStatus.FAILED),
                cutOffTime
        ).forEach(reconciliationService::processStuckNonMemberRefundItemSagaRetry);
    }

    // 사가가 완료되고 도메인에 반영되기 전에 서버가 멈춘 경우 처리
    private void bridgeCompletedSagas(LocalDateTime cutOffTime) {
        orderCreateSagaRepository.findAllByOverallStatusAndBridgedFalseAndUpdatedAtBefore(SagaStatus.COMPLETED, cutOffTime)
                .forEach(reconciliationService::compensateForCreateSagaBridgingFailure);

        orderCreateSagaRepository.findAllByOverallStatusAndBridgedFalseAndUpdatedAtBefore(SagaStatus.COMPLETED_COMPENSATED, cutOffTime)
                .forEach(reconciliationService::processCompletedCompensateSagaBridge);

        orderCancelSagaRepository.findAllByOverallStatusAndBridgedFalseAndUpdatedAtBefore(SagaStatus.COMPLETED, cutOffTime)
                .forEach(reconciliationService::processCompletedCancelSagaBridge);

        orderItemRefundSagaRepository.findAllByOverallStatusAndBridgedFalseAndUpdatedAtBefore(SagaStatus.COMPLETED, cutOffTime)
                .forEach(reconciliationService::processCompletedRefundSagaBridge);

        nonMemberOrderItemRefundSagaRepository.findAllByOverallStatusAndBridgedFalseAndUpdatedAtBefore(SagaStatus.COMPLETED, cutOffTime)
                .forEach(reconciliationService::processCompletedNonMemberRefundSagaBridge);
    }
}
