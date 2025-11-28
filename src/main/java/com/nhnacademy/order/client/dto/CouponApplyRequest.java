package com.nhnacademy.order.client.dto;

public record CouponApplyRequest(
    Long sagaId,
    Long memberId,
    Long couponId
) {}
