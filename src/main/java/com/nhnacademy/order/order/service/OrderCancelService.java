package com.nhnacademy.order.order.service;

import com.nhnacademy.order.common.aop.SagaIdContext;
import com.nhnacademy.order.common.context.SagaContext;
import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.domain.OrderStatus;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.ordersaga.cancellation.domain.CancelSagaStep;
import com.nhnacademy.order.ordersaga.cancellation.domain.OrderCancelSaga;
import com.nhnacademy.order.ordersaga.cancellation.repository.OrderCancelSagaRepository;
import com.nhnacademy.order.ordersaga.service.SagaUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class OrderCancelService {
    private final OrderRepository orderRepository;
    private final OrderCancelSagaRepository orderCancelSagaRepository;
    private final SagaUpdateService sagaUpdateService;

    @SagaIdContext
    @Transactional
    public OrderCancelSaga cancelStart(Order order) {
        if (order.getOrderStatus() == OrderStatus.CANCELING) {
            return orderCancelSagaRepository.findByOrderId(order.getOrderId())
                    .orElseThrow(() -> new IllegalStateException("주문의 상태는 'CANCELING'이지만, 사가가 존재하지 않는 오류 발생"));
        }

        order.setOrderStatus(OrderStatus.CANCELING);
        orderRepository.save(order);

        UUID sagaId = SagaContext.get();

        OrderCancelSaga saga = OrderCancelSaga.create(sagaId, order.getOrderId());
        sagaUpdateService.updateCancelSagaStep(saga, CancelSagaStep.STARTED);

        return saga;
    }

    @Transactional
    public void cancelOrder(Order order, OrderCancelSaga saga) {

        // 이미 처리된 주문은 다시 처리하지 않음
        if (order.getOrderStatus() == OrderStatus.CANCELED) {
            // 하지만 도메인과 연결되지 않았을 수 있으므로 처리
            if (!saga.isBridged()) {
                saga.setBridged(true);
                orderCancelSagaRepository.save(saga);
            }
            return;
        }

        order.setOrderStatus(OrderStatus.CANCELED);

        // 주문 상품도 모두 취소 처리
        order.getOrderItems().forEach(orderItem ->
                orderItem.setOrderItemStatus(OrderItemStatus.CANCELED)
        );

        orderRepository.save(order);

        saga.setBridged(true);

        orderCancelSagaRepository.save(saga);
    }
}
