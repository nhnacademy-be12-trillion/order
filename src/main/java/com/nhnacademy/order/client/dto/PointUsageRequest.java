package com.nhnacademy.order.client.dto;

public record PointUsageRequest(
    Long sagaId,
    Long memberId,
    int point
) {}
