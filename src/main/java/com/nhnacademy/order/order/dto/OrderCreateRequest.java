package com.nhnacademy.order.order.dto;

import com.nhnacademy.order.orderitem.dto.OrderItemCreateRequest;

import java.time.LocalDateTime;
import java.util.List;

public record OrderCreateRequest(
    String ordererName,
    String ordererContact,
    LocalDateTime deliveryDate,

    String receiverName,
    String receiverContact,
    String receiverAddress,
    String receiverPostCode,

    String nonMemberPassword,

    int pointUsage,

    Long couponId,

    List<OrderItemCreateRequest> orderItems
) {}
