package com.nhnacademy.order.order.domain;

import com.nhnacademy.order.common.entity.BaseTimeEntity;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.orderitem.exception.OrderItemNotFoundException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Order extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    // orderTitle 필요 없음 (주문 번호(orderNumber)로 대체)

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "non_member_password")
    private String nonMemberPassword; // 비회원 주문 조회를 위해 필요

    // isMember 필드는 불필요함 (memberId null 여부로 판단 가능)
    // isMember 필드가 존재한다면 memberId, isMember 두 필드 관리 필요

    @Setter
    @Column(name = "order_status")
    @Enumerated(value = EnumType.STRING)
    private OrderStatus orderStatus;

    @Embedded
    private OrdererInfo ordererInfo;

    @Embedded
    private ReceiverInfo receiverInfo;

    @Embedded
    private OrderDetails orderDetails;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    public static Order createInitial(Long memberId, String encryptedPassword, OrdererInfo ordererInfo, ReceiverInfo receiverInfo, OrderDetails orderDetails) {
        // UUID 대신 주문 일시(yyyyMMdd) + AUTO_INCREMENT된 식별자(orderId)?
        // createOrder의 repository.save()로 반환된 객체로 AUTO_INCREMENT된 식별자 획득 가능할듯?
        // 이후 주문 번호 업데이트 -> 변경 감지로 자동으로 DB에 반영

        String prefix = "ORD-";

        return new Order(
            null,
            prefix + UUID.randomUUID(),
            memberId,
            encryptedPassword,
            OrderStatus.CREATING,
            ordererInfo,
            receiverInfo,
            orderDetails,
            new ArrayList<>()
        );
    }

    public void completeOrder(int originPrice, int totalPrice, int deliveryFee) {
        this.orderDetails = this.orderDetails.withFinalValue(originPrice, totalPrice, deliveryFee);
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public OrderItem findOrderItemInOrder(Long orderItemId) {
        return this.getOrderItems().stream()
                .filter(orderItem -> orderItem.getOrderItemId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new OrderItemNotFoundException("존재하지 않는 주문 상품 ID: " + orderItemId));
    }

    public void reflectItemStatusChange() {
        int newTotalPrice = this.orderItems.stream()
                .filter(item -> item.getOrderItemStatus() != OrderItemStatus.CANCELED && item.getOrderItemStatus() != OrderItemStatus.RETURNED)
                .mapToInt(item -> item.getPrice() + item.getPackagingPrice())
                .sum();

        OrderDetails updatedOrderDetails = this.orderDetails.withNewTotalPrice(newTotalPrice);

        boolean allItemsCanceledOrReturned = this.orderItems.stream()
                .allMatch(item -> item.getOrderItemStatus() == OrderItemStatus.CANCELED || item.getOrderItemStatus() == OrderItemStatus.RETURNED);

        if (allItemsCanceledOrReturned) {
            this.orderStatus = OrderStatus.CANCELED;
            updatedOrderDetails = updatedOrderDetails.withNewDeliveryFee(0);
        }

        this.orderDetails = updatedOrderDetails;
    }
}
