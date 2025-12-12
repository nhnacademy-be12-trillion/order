package com.nhnacademy.order.orderitem.service;

import java.util.List;

public interface OrderItemService {
    List<Long> getTopNSellingBookIds(int limit);
}
