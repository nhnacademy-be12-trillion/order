package com.nhnacademy.order.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

@Embeddable
public record OrderDetails(
    @Column(name = "order_date")
    LocalDateTime orderDate,

    @Column(name = "shipping_post_code")
    String shippingPostCode, // 우편번호

    @Column(name = "delivery_date")
    LocalDateTime deliveryDate,

    @Column(name = "delivery_fee")
    int deliveryFee,

    @Column(name = "point_usage")
    int pointUsage,

    @Column(name = "origin_price")
    int originPrice,

    @Column(name = "total_price")
    int totalPrice
) {
    public static OrderDetails create(String shippingPostCode, LocalDateTime deliveryDate, int deliveryFee, int pointUsage, int originPrice, int totalPrice) {
        return new OrderDetails(
            LocalDateTime.now(),
            shippingPostCode,
            deliveryDate,
            deliveryFee,
            pointUsage,
            originPrice,
            totalPrice
        );
    }

    public OrderDetails withNewTotalPrice(int newTotalPrice) {
        return new OrderDetails(
            this.orderDate,
            this.shippingPostCode,
            this.deliveryDate,
            this.deliveryFee,
            this.pointUsage,
            this.originPrice,
            newTotalPrice
        );
    }

    public OrderDetails withNewDeliveryFee(int newDeliveryFee) {
        return new OrderDetails(
            this.orderDate,
            this.shippingPostCode,
            this.deliveryDate,
            newDeliveryFee,
            this.pointUsage,
            this.originPrice,
            this.totalPrice
        );
    }
}
