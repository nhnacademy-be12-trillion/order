package com.nhnacademy.order.order.dto;

import com.nhnacademy.order.order.domain.OrdererInfo;
import com.nhnacademy.order.order.domain.PaymentStatus;
import com.nhnacademy.order.order.domain.ReceiverInfo;

import java.time.LocalDateTime;

public record OrderBaseResponse(
    Long orderId,
    Long memberId,
    String orderTitle,
    LocalDateTime orderDate,
    PaymentStatus paymentStatus,
    int totalPrice,
    int deliveryFee,
    OrdererInfo ordererInfo,
    ReceiverInfo receiverInfo
) {}
