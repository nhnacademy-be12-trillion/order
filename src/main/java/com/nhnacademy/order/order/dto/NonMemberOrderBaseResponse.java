package com.nhnacademy.order.order.dto;

import com.nhnacademy.order.order.domain.OrdererInfo;
import com.nhnacademy.order.order.domain.OrderStatus;
import com.nhnacademy.order.order.domain.ReceiverInfo;

import java.time.LocalDateTime;

public record NonMemberOrderBaseResponse(
    Long orderId,
    String nonMemberPassword,
    Long memberId,
    String orderNumber,
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
            orderNumber,
            orderDate,
            orderStatus,
            totalPrice, // 비회원은 originPrice = totalPrice임
            totalPrice,
            deliveryFee,
            ordererInfo,
            receiverInfo
        );
    }
}
