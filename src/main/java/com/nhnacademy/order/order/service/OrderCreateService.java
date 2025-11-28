package com.nhnacademy.order.order.service;

import com.nhnacademy.order.order.domain.*;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 주문 생성 로직에서 트랜잭션과 관련 없는 연산이 너무 많아서 분리
@RequiredArgsConstructor
@Service
public class OrderCreateService {
    // Repository
    private final OrderRepository orderRepository;

    // 초기 주문 생성 - 보상 트랜잭션 도중 서버 종료 시 필요함
    @Transactional
    public Order createInitialOrder(Long memberId, String nonMemberPassword, OrdererInfo ordererInfo, ReceiverInfo receiverInfo, OrderDetails initialOrderDetails) {
        Order order = Order.createInitial(
                memberId,
                nonMemberPassword,
                ordererInfo,
                receiverInfo,
                initialOrderDetails
        );

        return orderRepository.save(order);
    }

    // 주문 생성 완료
    @Transactional
    public void completeOrder(Order order, int originPrice, int totalPrice, int deliveryFee, List<OrderItem> orderItems) {

        order.completeOrder(originPrice, totalPrice, deliveryFee);

        orderItems.forEach(order::addOrderItem);

        orderRepository.save(order);
    }

    // 주문 생성 실패
    @Transactional
    public void createFailureOrder(Order order) {
        order.setOrderStatus(OrderStatus.FAILED);
    }
}
