package com.nhnacademy.order.client.dto;

public record PointUsageRequest(
    Long memberId,
    int point
) {}
