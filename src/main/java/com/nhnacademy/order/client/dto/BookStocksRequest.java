package com.nhnacademy.order.client.dto;

import java.util.Map;

public record BookStocksRequest(
    Map<Long, Integer> quantityMap
) {}
