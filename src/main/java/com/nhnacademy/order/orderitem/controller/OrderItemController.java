package com.nhnacademy.order.orderitem.controller;

import org.springframework.http.ResponseEntity;

import java.util.List;

public interface OrderItemController {
    ResponseEntity<List<Long>> getTopNSellingBookIds(int limit);
}
