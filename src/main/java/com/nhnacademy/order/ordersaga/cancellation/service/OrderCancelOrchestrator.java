package com.nhnacademy.order.ordersaga.cancellation.service;

import com.nhnacademy.order.client.service.BookService;
import com.nhnacademy.order.client.service.CouponService;
import com.nhnacademy.order.client.service.MemberService;
import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.exception.OrderCancelFailureException;
import com.nhnacademy.order.order.service.OrderCancelService;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.ordersaga.cancellation.domain.CancelSagaStep;
import com.nhnacademy.order.ordersaga.cancellation.domain.OrderCancelSaga;
import com.nhnacademy.order.ordersaga.domain.SagaStatus;
import com.nhnacademy.order.ordersaga.service.SagaUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderCancelOrchestrator {
    private final SagaUpdateService sagaUpdateService;

    private final MemberService memberService;
    private final CouponService couponService;
    private final BookService bookService;
    // private final PaymentService paymentService;

    private final OrderCancelService orderCancelService;

    public void processCancelOrder(Long memberId, Order order) {
        // 1. 사가 시작 (사가 생성과 동시에 주문 상태를 '주문 취소 중'으로 변경 -> 사용자 경험 향상)
        OrderCancelSaga saga = orderCancelService.cancelStart(order);

        int pointUsage = order.getOrderDetails().pointUsage();

        Long couponId = order.getOrderDetails().couponId();

        Map<Long, Integer> quantityMap = order.getOrderItems().stream()
                .collect(Collectors.toMap(OrderItem::getBookId, OrderItem::getQuantity));

        try {
            // 2. 환불
            // TODO: PaymentService의 환불 로직 추가
            // paymentService.refund(...)
            sagaUpdateService.updateCancelSagaStep(saga, CancelSagaStep.PAYMENT_CANCELED);

            // 회원인 경우에만 (비회원은 쿠폰, 포인트 사용 불가능)
            if (memberId != null) {
                // 3. 멤버 API에 사용한 포인트만큼 증가 요청
                if (pointUsage > 0) {
                    memberService.increasePoint(memberId, pointUsage);
                    sagaUpdateService.updateCancelSagaStep(saga, CancelSagaStep.POINT_REFUNDED);
                }

                // 4. 쿠폰 API에 사용한 쿠폰 반환 요청
                if (couponId != null) {
                    couponService.withdrawCoupon(memberId, couponId);
                    sagaUpdateService.updateCancelSagaStep(saga, CancelSagaStep.COUPON_RESTORED);
                }
            }

            // 5. 도서 API에 재고 증가 요청
            bookService.increaseStocks(quantityMap);
            sagaUpdateService.updateCancelSagaStep(saga, CancelSagaStep.STOCK_INCREASED);

            // 6. 사가 성공
            sagaUpdateService.updateCancelSagaStatus(saga, SagaStatus.COMPLETED);

            // 7. 사가 - 도메인 브릿징 완료 (주문 상태 변경 -> 사가 브릿징 설정)
            orderCancelService.cancelOrder(order, saga);

        } catch (Exception e) {
            sagaUpdateService.updateCancelSagaStatus(saga, SagaStatus.FAILED);
            // 주문 취소나 환불은 고객이 결정한 시점에서 반드시 달성되어야 하는 요청 -> 보상 로직 필요 없음!
            // 재시도나 수동 개입을 통해 반드시 달성시켜야 함
            // 스케줄러를 사용해 재시도하거나, 배치 서버를 사용해 상태가 FAILED인 사가를 재시작
            throw new OrderCancelFailureException("주문 전체 취소 실패: " + order.getOrderId());
        }
    }

    public void retry(OrderCancelSaga saga, Order order) {
        // 이미 처리된 사가에 대해 재시도 하지 않음
        if (saga.getOverallStatus() == SagaStatus.COMPLETED) {
            return;
        }

        UUID sagaId = saga.getSagaId();

        Long memberId = order.getMemberId();

        int pointUsage = order.getOrderDetails().pointUsage();

        Long couponId = order.getOrderDetails().couponId();

        Map<Long, Integer> quantityMap = order.getOrderItems().stream()
                .collect(Collectors.toMap(OrderItem::getBookId, OrderItem::getQuantity));

        CancelSagaStep currentStep = saga.getLastCompletedStep();

        try {

            if (currentStep.ordinal() < CancelSagaStep.PAYMENT_CANCELED.ordinal()) {
                // TODO: 환불 로직 추가
                sagaUpdateService.updateCancelSagaStep(saga, CancelSagaStep.PAYMENT_CANCELED);

                currentStep = CancelSagaStep.PAYMENT_CANCELED;
            }

            if (memberId != null) {
                if (pointUsage > 0 && currentStep.ordinal() < CancelSagaStep.POINT_REFUNDED.ordinal()) {
                    memberService.increasePoint(memberId, pointUsage);
                    sagaUpdateService.updateCancelSagaStep(saga, CancelSagaStep.POINT_REFUNDED);

                    currentStep = CancelSagaStep.POINT_REFUNDED;
                }

                if (couponId != null && currentStep.ordinal() < CancelSagaStep.COUPON_RESTORED.ordinal()) {
                    couponService.withdrawCoupon(memberId, couponId);
                    sagaUpdateService.updateCancelSagaStep(saga, CancelSagaStep.COUPON_RESTORED);

                    currentStep = CancelSagaStep.COUPON_RESTORED;
                }
            }

            if (currentStep.ordinal() < CancelSagaStep.STOCK_INCREASED.ordinal()) {
                bookService.increaseStocks(quantityMap);
                sagaUpdateService.updateCancelSagaStep(saga, CancelSagaStep.STOCK_INCREASED);
            }

            sagaUpdateService.updateCancelSagaStatus(saga, SagaStatus.COMPLETED);

            orderCancelService.cancelOrder(order, saga);
        } catch (Exception e) {
            sagaUpdateService.updateCancelSagaStatus(saga, SagaStatus.FAILED);
            log.error("주문 취소 사가 재시도 실패: {}", sagaId, e);
        }
    }
}
