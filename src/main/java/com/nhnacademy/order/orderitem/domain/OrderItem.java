package com.nhnacademy.order.orderitem.domain;

import com.nhnacademy.order.order.domain.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orderitem_id")
    private Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "book_id")
    private Long bookId;

    private int quantity;

    private int price;

    @Column(name = "shipping_date")
    private LocalDateTime shippingDate; // 출고일

    @Column(name = "packaging_price")
    private int packagingPrice;

    @Column(name = "orderitem_status")
    private OrderItemStatus orderItemStatus;

    @Column(name = "coupon_id")
    private Long couponId;

    public static OrderItem create(Order order, Long bookId, int quantity, int price, int packagingPrice, Long couponId) {
        return new OrderItem(
            null,
            order,
            bookId,
            quantity,
            price,
            null, // 출고일 - 관리자가 설정
            packagingPrice,
            OrderItemStatus.PREPARING,
            couponId
        );
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public void setOrderItemStatus(OrderItemStatus orderItemStatus) {
        this.orderItemStatus = orderItemStatus;
    }

    public void ship() {
        this.orderItemStatus = OrderItemStatus.SHIPPED;
        this.shippingDate = LocalDateTime.now();
    }
}
