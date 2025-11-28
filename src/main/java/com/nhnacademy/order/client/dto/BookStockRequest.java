package com.nhnacademy.order.client.dto;

import java.util.Map;

public record BookStockRequest(
    Long sagaId,
    Map<Long, Integer> quantityMap
) {}
