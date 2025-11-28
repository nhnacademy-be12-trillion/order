package com.nhnacademy.order.order.dto;

import com.nhnacademy.order.order.domain.OrdererInfo;
import com.nhnacademy.order.order.domain.OrderStatus;
import com.nhnacademy.order.order.domain.ReceiverInfo;

import java.time.LocalDateTime;

public record NonMemberBaseResponse(
    Long orderId,
    String nonMemberPassword,
    Long memberId,
    String orderTitle,
    LocalDateTime orderDate,
    OrderStatus orderStatus,
    int totalPrice,
    int deliveryFee,
    OrdererInfo ordererInfo,
    ReceiverInfo receiverInfo
) {
    public OrderBaseResponse toOrderBaseResponse() {
        return new OrderBaseResponse(
            orderId,
            memberId,
            orderTitle,
            orderDate,
            orderStatus,
            totalPrice,
            deliveryFee,
            ordererInfo,
            receiverInfo
        );
    }
}
