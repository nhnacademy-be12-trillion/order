package com.nhnacademy.order.orderitem.dto;

import java.time.LocalDateTime;

public record OrderItemCreateRequest(
    Long bookId,
    int quantity,
    Long couponId,
    Long packagingId,
    LocalDateTime shippingDate
) {}
