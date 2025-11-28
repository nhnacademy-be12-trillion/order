package com.nhnacademy.order.client.dto;

public record CouponGetDiscountAmountRequest(
    Long couponId,
    int price
) {}
