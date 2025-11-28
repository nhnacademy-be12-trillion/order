package com.nhnacademy.order.orderitem.dto;

import com.nhnacademy.order.orderitem.domain.OrderItemStatus;

public record OrderItemResponse(
    Long orderId,
    Long bookId,
    int quantity,
    int price,
    int packagingPrice,
    OrderItemStatus orderItemStatus
) {}
