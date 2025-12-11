package com.nhnacademy.order.ordersaga.itemrefund.service;

import com.nhnacademy.order.client.service.BookService;
import com.nhnacademy.order.client.service.CouponService;
import com.nhnacademy.order.client.service.MemberService;
import com.nhnacademy.order.common.aop.SagaIdContext;
import com.nhnacademy.order.common.context.SagaContext;
import com.nhnacademy.order.delivery.domain.DeliveryPolicy;
import com.nhnacademy.order.delivery.exception.PolicyNotConfiguredException;
import com.nhnacademy.order.delivery.repository.DeliveryPolicyRepository;
import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.exception.OrderStatusTransitionException;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.orderitem.service.OrderItemRefundService;
import com.nhnacademy.order.ordersaga.domain.SagaStatus;
import com.nhnacademy.order.ordersaga.itemrefund.domain.OrderItemRefundSaga;
import com.nhnacademy.order.ordersaga.itemrefund.domain.ItemRefundSagaStep;
import com.nhnacademy.order.orderitem.exception.OrderItemRefundFailureException;
import com.nhnacademy.order.ordersaga.service.SagaUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderItemRefundOrchestrator {
    private final SagaUpdateService sagaUpdateService;
    private final MemberService memberService;
    private final CouponService couponService;
    private final BookService bookService;

    private final DeliveryPolicyRepository deliveryPolicyRepository;
    private final OrderItemRefundService orderItemRefundService;

    @SagaIdContext
    public void processItemRefund(Long memberId, Order order, OrderItem orderItem) {
        if (orderItem.getOrderItemStatus() != OrderItemStatus.RETURN_REQUESTED_CHANGE_OF_MIND &&
        orderItem.getOrderItemStatus() != OrderItemStatus.RETURN_REQUESTED_DAMAGED) {
            throw new OrderStatusTransitionException("반품 요청 상태가 아닌 상품: " + orderItem.getOrderItemId());
        }

        UUID sagaId = SagaContext.get();

        OrderItemRefundSaga saga = OrderItemRefundSaga.create(sagaId, order.getOrderId(), orderItem.getOrderItemId());

        // 1. 사가 시작
        sagaUpdateService.updateItemRefundSagaStep(saga, ItemRefundSagaStep.STARTED);

        // 단순하게 API를 유지하기 위해서 단일 상품임에도 맵을 만들어서 사용
        Map<Long, Integer> quantityMap = Map.of(orderItem.getOrderItemId(), orderItem.getQuantity());

        // 상품 가격 * 개수
        int refundPoint = orderItem.getPrice() * orderItem.getQuantity();

        // 배송비
        int deliveryFee = getDeliveryFee();

        // 환불 이유가 단순 변심인 경우
        if (orderItem.getOrderItemStatus().equals(OrderItemStatus.RETURN_REQUESTED_CHANGE_OF_MIND)) {
            // 반품 배송비를 포인트에서 차감한 후 지급
            refundPoint = Math.max(0, refundPoint - deliveryFee);
        }

        try {
            // TODO: 쿠폰 조건(ex) 30,000원 이상 구매 시 10,000원 할인)이 깨진 경우 환불 포인트에서 차감 (단, 판매자 귀책인 경우 전액 환불)

            // 2. 멤버 API에 포인트 증가 요청
            memberService.increasePoint(memberId, refundPoint);
            sagaUpdateService.updateItemRefundSagaStep(saga, ItemRefundSagaStep.POINT_REFUNDED);

            // 3. 도서 API에 재고 증가 요청
            bookService.increaseStocks(quantityMap);
            sagaUpdateService.updateItemRefundSagaStep(saga, ItemRefundSagaStep.STOCK_INCREASED);

            // 4. 사가 성공
            sagaUpdateService.updateItemRefundSagaStatus(saga, SagaStatus.COMPLETED);

            // 5. 도메인과 연결
            orderItemRefundService.completeOrderItem(orderItem, saga);
        } catch (Exception e) {
            sagaUpdateService.updateItemRefundSagaStatus(saga, SagaStatus.FAILED);
            throw new OrderItemRefundFailureException("주문 상품 환불 실패: " + orderItem.getOrderItemId());
        }
    }

    public void retry(OrderItemRefundSaga saga, Order order, OrderItem orderItem) {
        if (saga.getOverallStatus() == SagaStatus.COMPLETED) {
            return;
        }

        UUID sagaId = saga.getSagaId();

        Map<Long, Integer> quantityMap = Map.of(orderItem.getOrderItemId(), orderItem.getQuantity());

        Long memberId = order.getMemberId();

        int refundPoint = orderItem.getPrice() * orderItem.getQuantity();

        int deliveryFee = getDeliveryFee();

        if (orderItem.getOrderItemStatus() == OrderItemStatus.RETURN_REQUESTED_CHANGE_OF_MIND) {
            refundPoint = Math.max(0, refundPoint - deliveryFee);
        }

        ItemRefundSagaStep currentStep = saga.getLastCompletedStep();

        try {
            // TODO: 쿠폰 조건이 깨진 경우 환불 포인트에서 차감 (단순 변심 환불인 경우에만)

            if (currentStep.ordinal() < ItemRefundSagaStep.POINT_REFUNDED.ordinal()) {
                memberService.increasePoint(memberId, refundPoint);
                sagaUpdateService.updateItemRefundSagaStep(saga, ItemRefundSagaStep.POINT_REFUNDED);

                currentStep = ItemRefundSagaStep.POINT_REFUNDED;
            }

            if (currentStep.ordinal() < ItemRefundSagaStep.STOCK_INCREASED.ordinal()) {
                bookService.increaseStocks(quantityMap);
                sagaUpdateService.updateItemRefundSagaStep(saga, ItemRefundSagaStep.STOCK_INCREASED);
            }

            sagaUpdateService.updateItemRefundSagaStatus(saga, SagaStatus.COMPLETED);

            orderItemRefundService.completeOrderItem(orderItem, saga);
        } catch (Exception e) {

            sagaUpdateService.updateItemRefundSagaStatus(saga, SagaStatus.FAILED);
            log.error("회원 주문 상품 환불 사가 재시도 실패: {}", sagaId, e);
        }
    }

    private int getDeliveryFee() {
        DeliveryPolicy deliveryPolicy = deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc()
                .orElseThrow(() -> new PolicyNotConfiguredException("배송 정책이 설정되지 않음"));

        return deliveryPolicy.getDeliveryPolicyFee();
    }
}
