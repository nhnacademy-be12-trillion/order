package com.nhnacademy.order.ordersaga.creation.service;

import com.nhnacademy.order.client.service.BookService;
import com.nhnacademy.order.client.service.CouponService;
import com.nhnacademy.order.client.service.MemberService;
import com.nhnacademy.order.order.domain.*;
import com.nhnacademy.order.order.exception.OrderCreateFailureException;
import com.nhnacademy.order.order.service.OrderCompensateService;
import com.nhnacademy.order.ordersaga.service.SagaUpdateService;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.ordersaga.creation.domain.CreateSagaStep;
import com.nhnacademy.order.ordersaga.creation.domain.OrderCreateSaga;
import com.nhnacademy.order.ordersaga.domain.SagaStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OrderCreateOrchestrator {
    private final SagaUpdateService sagaUpdateService;

    private final MemberService memberService;
    private final CouponService couponService;
    private final BookService bookService;
    private final OrderCompensateService orderCompensateService;

    public void processCreateOrder(OrderCreateSaga saga, Order order) {
        // 1. 사가 시작
        sagaUpdateService.updateCreateSagaStep(saga, CreateSagaStep.STARTED);

        Long memberId = order.getMemberId();

        int pointUsage = order.getOrderDetails().pointUsage();

        Long couponId = order.getOrderDetails().couponId();

        // List<OrderItemCreateRequest> -> Map<bookId, quantity> 추출
        Map<Long, Integer> quantityMap = order.getOrderItems().stream()
                .collect(Collectors.toMap(OrderItem::getBookId, OrderItem::getQuantity));

        try {
            // 2. 재고 감소 중
            sagaUpdateService.updateCreateSagaStep(saga, CreateSagaStep.STOCK_DECREASING);

            // 3. 도서 API에 재고 감소 요청
            bookService.decreaseStocks(quantityMap);

            // 4. 사가 상태 업데이트 (재고 감소 성공)
            sagaUpdateService.updateCreateSagaStep(saga, CreateSagaStep.STOCK_DECREASED);

            if (couponId != null) {
                // 5. 쿠폰 사용 중
                sagaUpdateService.updateCreateSagaStep(saga, CreateSagaStep.COUPON_APPLYING);

                // 6. 쿠폰 ID가 존재하면 쿠폰 API에 쿠폰 적용 요청
                couponService.applyCoupon(memberId, couponId);

                // 7. 사가 상태 업데이트 (쿠폰 적용)
                sagaUpdateService.updateCreateSagaStep(saga, CreateSagaStep.COUPON_APPLIED);
            }

            if (pointUsage > 0) {
                // 8. 포인트 사용 중
                sagaUpdateService.updateCreateSagaStep(saga, CreateSagaStep.POINT_USING);

                // 9. 사용 포인트가 존재하면 멤버 API에 포인트 감소 요청
                memberService.decreasePoint(memberId, pointUsage);

                // 10. 사가 상태 업데이트 (포인트 감소 성공)
                sagaUpdateService.updateCreateSagaStep(saga, CreateSagaStep.POINT_USED);
            }

            // 11. 사가 성공
            sagaUpdateService.updateCreateSagaStatus(saga, SagaStatus.COMPLETED);

        } catch (Exception e) {
            // 12. 보상 트랜잭션 시작
            compensate(saga, order);

            // 13. RestControllerAdvice에서 ErrorResponse 생성을 위한 예외 던지기
            throw new OrderCreateFailureException("주문 생성 실패" + order.getOrderId());
        }
    }

    public void compensate(OrderCreateSaga saga, Order order) {
        // 이미 처리된 사가에 대해 재시도 하지 않음
        if (saga.getOverallStatus() == SagaStatus.COMPLETED_COMPENSATED) {
            return;
        }

        // 1. 사가의 상태를 COMPENSATE로 설정 (보상 트랜잭션 진행 중 서버 오류에도 다시 동작하기 위함)
        sagaUpdateService.updateCreateSagaStatus(saga, SagaStatus.COMPENSATED);

        int pointUsage = order.getOrderDetails().pointUsage();
        Long memberId = order.getMemberId();
        Long couponId = order.getOrderDetails().couponId();

        Map<Long, Integer> quantityMap = order.getOrderItems().stream()
                .collect(Collectors.toMap(OrderItem::getBookId, OrderItem::getQuantity));

        // 2. 마지막으로 수행한 사가 단계 확인
        CreateSagaStep currentStep = saga.getLastCompletedStep();

        // 3. 역순으로 보상 트랜잭션 시작
        if (currentStep == CreateSagaStep.POINT_USING || currentStep == CreateSagaStep.POINT_USED) {
            if (pointUsage > 0) {
                memberService.rollbackPoint(memberId, pointUsage);
            }
            sagaUpdateService.updateCreateSagaStep(saga, CreateSagaStep.COUPON_APPLIED);

            currentStep = CreateSagaStep.COUPON_APPLIED;
        }

        if (currentStep == CreateSagaStep.COUPON_APPLYING || currentStep == CreateSagaStep.COUPON_APPLIED) {
            if (couponId != null) {
                couponService.withdrawCoupon(memberId, couponId);
            }
            sagaUpdateService.updateCreateSagaStep(saga, CreateSagaStep.STOCK_DECREASED);

            currentStep = CreateSagaStep.STOCK_DECREASED;
        }

        if (currentStep == CreateSagaStep.STOCK_DECREASING || currentStep == CreateSagaStep.STOCK_DECREASED) {
            bookService.rollbackStocks(quantityMap);
        }
        sagaUpdateService.updateCreateSagaStep(saga, CreateSagaStep.STARTED);

        // 4. 보상 트랜잭션 완료 (COMPENSATE -> COMPLETED_COMPENSATED)
        sagaUpdateService.updateCreateSagaStatus(saga, SagaStatus.COMPLETED_COMPENSATED);

        // 5. 사가 - 도메인 연결
        orderCompensateService.compensateOrder(order, saga);
    }
}
