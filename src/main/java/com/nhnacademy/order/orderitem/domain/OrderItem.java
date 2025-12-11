package com.nhnacademy.order.orderitem.domain;

import com.nhnacademy.order.common.entity.BaseTimeEntity;
import com.nhnacademy.order.order.domain.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "orderitem_id")
    private Long orderItemId;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "book_id")
    private Long bookId;

    private String bookName;

    private int quantity;

    private Integer price;

    @Column(name = "shipping_date")
    private LocalDateTime shippingDate; // 출고일

    @Column(name = "packaging_price")
    private int packagingPrice;

    @Setter
    @Column(name = "orderitem_status")
    @Enumerated(value = EnumType.STRING)
    private OrderItemStatus orderItemStatus;

    public static OrderItem createInitial(Order order, Long bookId, int quantity, LocalDateTime shippingDate, int packagingPrice) {
        return new OrderItem(
            order,
            bookId,
            null,
            quantity,
            null,
            shippingDate,
            packagingPrice
        );
    }

    private OrderItem(Order order, Long bookId, String bookName, int quantity, Integer price, LocalDateTime shippingDate, Integer packagingPrice) {
        this.orderItemId = null;
        this.order = order;
        this.bookId = bookId;
        this.bookName = bookName;
        this.quantity = quantity;
        this.price = price;
        this.shippingDate = shippingDate;
        this.packagingPrice = packagingPrice;
        this.orderItemStatus = OrderItemStatus.PREPARING;
    }

    public void completeOrderItem(String bookName, int price) {
        this.bookName = bookName;
        this.price = price;
    }

    public void ship() {
        this.orderItemStatus = OrderItemStatus.SHIPPED;
        if (this.shippingDate == null) {
            this.shippingDate = LocalDateTime.now();
        }
    }
}
