package com.nhnacademy.order.order.dto;

import com.nhnacademy.order.order.domain.OrdererInfo;
import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.domain.PaymentStatus;
import com.nhnacademy.order.order.domain.ReceiverInfo;
import com.nhnacademy.order.orderitem.dto.OrderItemResponse;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long orderId,
    Long memberId,
    String orderNumber,
    LocalDateTime orderDate,
    PaymentStatus paymentStatus,
    int totalPrice,
    int deliveryFee,
    OrdererInfo ordererInfo,
    ReceiverInfo receiverInfo,
    List<OrderItemResponse> orderItems
) {
    public static OrderResponse create(OrderBaseResponse orderBaseResponse, List<OrderItemResponse> orderItems) {
        return new OrderResponse(
            orderBaseResponse.orderId(),
            orderBaseResponse.memberId(),
            orderBaseResponse.orderTitle(),
            orderBaseResponse.orderDate(),
            orderBaseResponse.paymentStatus(),
            orderBaseResponse.totalPrice(),
            orderBaseResponse.deliveryFee(),
            orderBaseResponse.ordererInfo(),
            orderBaseResponse.receiverInfo(),
            orderItems
        );
    }

    public static OrderResponse create(Order order, List<OrderItemResponse> orderItems) {
        return new OrderResponse(
            order.getOrderId(),
            order.getMemberId(),
            order.getOrderNumber(),
            order.getOrderDetails().orderDate(),
            order.getPaymentStatus(),
            order.getOrderDetails().totalPrice(),
            order.getOrderDetails().deliveryFee(),
            order.getOrdererInfo(),
            order.getReceiverInfo(),
            orderItems
        );
    }
}
