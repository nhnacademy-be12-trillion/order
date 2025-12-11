package com.nhnacademy.order.order.service;

import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.domain.OrderStatus;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.ordersaga.creation.domain.OrderCreateSaga;
import com.nhnacademy.order.ordersaga.creation.repository.OrderCreateSagaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OrderCompensateService {
    private final OrderRepository orderRepository;
    private final OrderCreateSagaRepository orderCreateSagaRepository;

    @Transactional
    public void compensateOrder(Order order, OrderCreateSaga saga) {
        if (order.getOrderStatus() == OrderStatus.CREATION_FAILED) {
            if (!saga.isBridged()) {
                saga.setBridged(true);
                orderCreateSagaRepository.save(saga);
            }
            return;
        }

        order.setOrderStatus(OrderStatus.CREATION_FAILED);

        orderRepository.save(order);

        saga.setBridged(true);

        orderCreateSagaRepository.save(saga);
    }
}

