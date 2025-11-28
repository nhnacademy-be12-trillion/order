package com.nhnacademy.order.orderitem.dto;

import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;

public record OrderItemResponse(
    Long orderId,
    Long bookId,
    int quantity,
    int price,
    int packagingPrice,
    OrderItemStatus orderItemStatus
) {
    public static OrderItemResponse create(OrderItem orderItem) {
        return new OrderItemResponse(
            orderItem.getOrder().getOrderId(),
            orderItem.getBookId(),
            orderItem.getQuantity(),
            orderItem.getPrice(),
            orderItem.getPackagingPrice(),
            orderItem.getOrderItemStatus()
        );
    }
}
