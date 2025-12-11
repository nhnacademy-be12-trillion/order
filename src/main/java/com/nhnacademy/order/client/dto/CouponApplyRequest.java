package com.nhnacademy.order.client.dto;

public record CouponApplyRequest(
    Long memberId,
    Long couponId
) {}
