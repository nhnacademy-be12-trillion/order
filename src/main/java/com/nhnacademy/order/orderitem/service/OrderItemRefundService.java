package com.nhnacademy.order.orderitem.service;

import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.orderitem.repository.OrderItemRepository;
import com.nhnacademy.order.ordersaga.itemrefund.domain.NonMemberOrderItemRefundSaga;
import com.nhnacademy.order.ordersaga.itemrefund.domain.OrderItemRefundSaga;
import com.nhnacademy.order.ordersaga.itemrefund.repository.NonMemberOrderItemRefundSagaRepository;
import com.nhnacademy.order.ordersaga.itemrefund.repository.OrderItemRefundSagaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OrderItemRefundService {
    private final OrderItemRepository orderItemRepository;
    private final OrderItemRefundSagaRepository orderItemRefundSagaRepository;
    private final NonMemberOrderItemRefundSagaRepository nonMemberOrderItemRefundSagaRepository;

    @Transactional
    public void completeOrderItem(OrderItem orderItem, OrderItemRefundSaga saga) {
        if (orderItem.getOrderItemStatus() == OrderItemStatus.RETURNED) {
            if (!saga.isBridged()) {
                saga.setBridged(true);
                orderItemRefundSagaRepository.save(saga);
            }
            return;
        }

        orderItem.setOrderItemStatus(OrderItemStatus.RETURNED);

        orderItemRepository.save(orderItem);

        saga.setBridged(true);

        orderItemRefundSagaRepository.save(saga);
    }

    @Transactional
    public void completeNonMemberOrderItem(OrderItem orderItem, NonMemberOrderItemRefundSaga saga) {
        if (orderItem.getOrderItemStatus() == OrderItemStatus.RETURNED) {
            if (!saga.isBridged()) {
                saga.setBridged(true);
                nonMemberOrderItemRefundSagaRepository.save(saga);
            }
            return;
        }

        orderItem.setOrderItemStatus(OrderItemStatus.RETURNED);

        orderItemRepository.save(orderItem);

        saga.setBridged(true);

        nonMemberOrderItemRefundSagaRepository.save(saga);
    }
}
