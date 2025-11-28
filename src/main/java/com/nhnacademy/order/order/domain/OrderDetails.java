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

    // 총 상품 금액: (모든 도서 가격 * 수량) + 모든 도서의 포장비 합
    @Column(name = "origin_price")
    int originPrice,

    // 최종 청구 금액: originPrice - (총 쿠폰 할인액 + 사용 포인트) + 배송비
    @Column(name = "total_price")
    int totalPrice,

    @Column(name = "coupon_id")
    Long couponId
) {
    public static OrderDetails createInitial(String shippingPostCode, LocalDateTime deliveryDate, int pointUsage, Long couponId) {
        return new OrderDetails(
            LocalDateTime.now(),
            shippingPostCode,
            deliveryDate,
            0,  // 확정된 배송비로 교체
            pointUsage,
            0,  // 확정된 초기 결제액으로 교체
            0,            // 확정된 최종 결제액으로 교체
            couponId
        );
    }

    public OrderDetails withFinalValue(int originPrice, int totalPrice, int deliveryFee) {
        return new OrderDetails(
            this.orderDate,
            this.shippingPostCode,
            this.deliveryDate,
            deliveryFee,
            this.pointUsage,
            originPrice,
            totalPrice,
            this.couponId
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
            newTotalPrice,
            this.couponId
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
            this.totalPrice,
            this.couponId
        );
    }
}
