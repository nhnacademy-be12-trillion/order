package com.nhnacademy.order.ordersaga.itemrefund.service;

import com.nhnacademy.order.client.service.BookService;
import com.nhnacademy.order.common.aop.SagaIdContext;
import com.nhnacademy.order.common.context.SagaContext;
import com.nhnacademy.order.delivery.domain.DeliveryPolicy;
import com.nhnacademy.order.delivery.exception.PolicyNotConfiguredException;
import com.nhnacademy.order.delivery.repository.DeliveryPolicyRepository;
import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.exception.OrderStatusTransitionException;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.orderitem.exception.OrderItemRefundFailureException;
import com.nhnacademy.order.orderitem.service.OrderItemRefundService;
import com.nhnacademy.order.ordersaga.domain.SagaStatus;
import com.nhnacademy.order.ordersaga.itemrefund.domain.NonMemberOrderItemRefundSaga;
import com.nhnacademy.order.ordersaga.itemrefund.domain.NonMemberRefundSagaStep;
import com.nhnacademy.order.ordersaga.service.SagaUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class NonMemberOrderItemRefundOrchestrator {
    private final SagaUpdateService sagaUpdateService;
    private final BookService bookService;
    // private final PaymentService paymentService;

    private final DeliveryPolicyRepository deliveryPolicyRepository;
    private final OrderItemRefundService orderItemRefundService;

    @SagaIdContext
    public void processNonMemberItemRefund(Order order, OrderItem orderItem) {
        if (orderItem.getOrderItemStatus() != OrderItemStatus.RETURN_REQUESTED_CHANGE_OF_MIND &&
                orderItem.getOrderItemStatus() != OrderItemStatus.RETURN_REQUESTED_DAMAGED) {
            throw new OrderStatusTransitionException("반품 요청 상태가 아닌 상품: " + orderItem.getOrderItemId());
        }

        UUID sagaId = SagaContext.get();

        NonMemberOrderItemRefundSaga saga = NonMemberOrderItemRefundSaga.create(sagaId, order.getOrderId(), orderItem.getOrderItemId());

        int deliveryFee = getDeliveryFee();

        Map<Long, Integer> quantityMap = Map.of(orderItem.getOrderItemId(), orderItem.getQuantity());

        // TODO: 환불 요청 DTO에서 quantity도 받아서 처리?

        int refundMoney = Math.max(orderItem.getPrice() - deliveryFee, 0);

        // 1. 사가 시작
        sagaUpdateService.updateNonMemberItemRefundSagaStep(saga, NonMemberRefundSagaStep.STARTED);

        try {
            // 2. 환불
            // TODO: 환불 로직 수정
            // paymentService.refundPayment(...)
            sagaUpdateService.updateNonMemberItemRefundSagaStep(saga, NonMemberRefundSagaStep.PAYMENT_REFUNDED);

            bookService.increaseStocks(quantityMap);
            sagaUpdateService.updateNonMemberItemRefundSagaStep(saga, NonMemberRefundSagaStep.STOCK_INCREASED);

            sagaUpdateService.updateNonMemberItemRefundSagaStatus(saga, SagaStatus.COMPLETED);

            orderItemRefundService.completeNonMemberOrderItem(orderItem, saga);
        } catch (Exception e) {
            sagaUpdateService.updateNonMemberItemRefundSagaStatus(saga, SagaStatus.FAILED);
            throw new OrderItemRefundFailureException("주문 상품 환불 실패: " + orderItem.getOrderItemId());
        }
    }

    public void retry(NonMemberOrderItemRefundSaga saga, OrderItem orderItem) {
        if (saga.getOverallStatus() == SagaStatus.COMPLETED) {
            return;
        }

        UUID sagaId = saga.getSagaId();

        Map<Long, Integer> quantityMap = Map.of(orderItem.getOrderItemId(), orderItem.getQuantity());

        int deliveryFee = getDeliveryFee();

        int refundMoney = Math.max(0, orderItem.getPrice() - deliveryFee);

        NonMemberRefundSagaStep currentStep = saga.getLastCompletedStep();

        try {
            if (currentStep.ordinal() < NonMemberRefundSagaStep.PAYMENT_REFUNDED.ordinal()) {
                // TODO: 환불 로직
                sagaUpdateService.updateNonMemberItemRefundSagaStep(saga, NonMemberRefundSagaStep.PAYMENT_REFUNDED);

                currentStep = NonMemberRefundSagaStep.PAYMENT_REFUNDED;
            }

            if (currentStep.ordinal() < NonMemberRefundSagaStep.STOCK_INCREASED.ordinal()) {
                bookService.increaseStocks(quantityMap);

                sagaUpdateService.updateNonMemberItemRefundSagaStep(saga, NonMemberRefundSagaStep.STOCK_INCREASED);
            }

            sagaUpdateService.updateNonMemberItemRefundSagaStatus(saga, SagaStatus.COMPLETED);

            orderItemRefundService.completeNonMemberOrderItem(orderItem, saga);
        } catch (Exception e) {
            sagaUpdateService.updateNonMemberItemRefundSagaStatus(saga, SagaStatus.FAILED);
            log.error("비회원 주문 상품 환불 사가 재시도 실패: {}", sagaId, e);
        }
    }

    private int getDeliveryFee() {
        DeliveryPolicy deliveryPolicy = deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc()
                .orElseThrow(() -> new PolicyNotConfiguredException("배송 정책이 설정되지 않음"));

        return deliveryPolicy.getDeliveryPolicyFee();
    }
}
