package com.nhnacademy.order.order.service;

import com.nhnacademy.order.client.service.BookService;
import com.nhnacademy.order.client.service.CouponService;
import com.nhnacademy.order.client.service.MemberService;
import com.nhnacademy.order.order.domain.*;
import com.nhnacademy.order.order.exception.OrderCreateFailureException;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OrderOrchestrator {
    private final SagaUpdateService sagaUpdateService;

    private final MemberService memberService;
    private final CouponService couponService;
    private final BookService bookService;

    public void processOrder(Long memberId, Order order) {
        OrderSaga orderSaga = OrderSaga.create(order.getOrderId());

        // 1. 사가 시작
        sagaUpdateService.updateSagaStep(orderSaga, SagaStep.STARTED);

        Long sagaId = orderSaga.getSagaId();

        int pointUsage = order.getOrderDetails().pointUsage();

        Long couponId = order.getOrderDetails().couponId();

        // List<OrderItemCreateRequest> -> Map<bookId, quantity> 추출
        Map<Long, Integer> quantityMap = order.getOrderItems().stream()
                .collect(Collectors.toMap(OrderItem::getBookId, OrderItem::getQuantity));

        try {
            // 2. 도서 API에 재고 감소 요청
            bookService.decreaseStock(sagaId, quantityMap);

            // 3. 사가 상태 업데이트 (재고 감소 성공)
            sagaUpdateService.updateSagaStep(orderSaga, SagaStep.STOCK_DECREASED);

            if (Objects.nonNull(couponId)) {
                // 4. 쿠폰 ID가 존재하면 쿠폰 API에 쿠폰 적용 요청
                couponService.applyCoupon(sagaId, memberId, couponId);

                // 5. 사가 상태 업데이트 (쿠폰 적용)
                sagaUpdateService.updateSagaStep(orderSaga, SagaStep.COUPON_APPLIED);
            }

            if (pointUsage > 0) {
                // 6. 사용 포인트가 존재하면 멤버 API에 포인트 감소 요청
                memberService.decreasePoint(sagaId, memberId, pointUsage);

                // 7. 사가 상태 업데이트 (포인트 감소 성공)
                sagaUpdateService.updateSagaStep(orderSaga, SagaStep.POINT_USED);
            }

            // 8. 사가 성공
            sagaUpdateService.updateSagaStatus(orderSaga, SagaStatus.COMPLETED);

        } catch (Exception e) {
            // 9. 사가의 상태를 COMPENSATE로 설정 (보상 트랜잭션 진행 중 서버 오류에도 다시 동작하기 위함)
            sagaUpdateService.updateSagaStatus(orderSaga, SagaStatus.COMPENSATED);

            // 10. 보상 트랜잭션 시작
            compensate(orderSaga, memberId, quantityMap, couponId, pointUsage);

            // 11. RestControllerAdvice에서 ErrorResponse 생성을 위한 예외 던지기
            throw new OrderCreateFailureException("주문 생성 실패");
        }
    }

    private void compensate(OrderSaga orderSaga, Long memberId, Map<Long, Integer> quantityMap, Long couponId, int pointUsage) {
        // 1. 마지막으로 수행한 사가 단계 확인
        SagaStep currentStep = orderSaga.getLastCompletedStep();

        Long sagaId = orderSaga.getSagaId();

        // 2. 역순으로 보상 트랜잭션 시작
        if (currentStep == SagaStep.POINT_USED) {
            if (pointUsage > 0) {
                memberService.increasePoint(sagaId, memberId, pointUsage);
            }
            sagaUpdateService.updateSagaStep(orderSaga, SagaStep.COUPON_APPLIED);

            currentStep = SagaStep.COUPON_APPLIED;
        }

        if (currentStep == SagaStep.COUPON_APPLIED) {
            if (couponId != null) {
                couponService.withdrawCoupon(sagaId, memberId, couponId);
            }
            sagaUpdateService.updateSagaStep(orderSaga, SagaStep.STOCK_DECREASED);

            currentStep = SagaStep.STOCK_DECREASED;
        }

        if (currentStep == SagaStep.STOCK_DECREASED) {
            bookService.increaseStock(sagaId, quantityMap);
        }

        // 3. 보상 트랜잭션 완료 (COMPENSATE -> FAILED)
        sagaUpdateService.updateSagaStatus(orderSaga, SagaStatus.FAILED);
    }
}
