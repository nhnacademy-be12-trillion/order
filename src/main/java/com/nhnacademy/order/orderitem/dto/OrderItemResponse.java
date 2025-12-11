package com.nhnacademy.order.orderitem.dto;

import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;

public record OrderItemResponse(
    Long orderItemId,
    Long orderId,
    Long bookId,
    String bookName,
    int quantity,
    int price,
    int packagingPrice,
    OrderItemStatus orderItemStatus
) {
    public static OrderItemResponse create(OrderItem orderItem) {
        return new OrderItemResponse(
            orderItem.getOrderItemId(),
            orderItem.getOrder().getOrderId(),
            orderItem.getBookId(),
            orderItem.getBookName(),
            orderItem.getQuantity(),
            orderItem.getPrice(),
            orderItem.getPackagingPrice(),
            orderItem.getOrderItemStatus()
        );
    }
}
