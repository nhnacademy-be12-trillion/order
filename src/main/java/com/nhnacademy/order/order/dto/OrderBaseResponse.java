package com.nhnacademy.order.order.dto;

import com.nhnacademy.order.order.domain.OrdererInfo;
import com.nhnacademy.order.order.domain.OrderStatus;
import com.nhnacademy.order.order.domain.ReceiverInfo;

import java.time.LocalDateTime;

public record OrderBaseResponse(
    Long orderId,
    Long memberId,
    String orderNumber,
    LocalDateTime orderDate,
    OrderStatus orderStatus,
    int originPrice,
    int totalPrice,
    int deliveryFee,
    OrdererInfo ordererInfo,
    ReceiverInfo receiverInfo
) {}
