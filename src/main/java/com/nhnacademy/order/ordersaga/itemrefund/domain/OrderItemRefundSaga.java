package com.nhnacademy.order.ordersaga.itemrefund.domain;

import com.nhnacademy.order.ordersaga.domain.OrderSaga;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "order_item_refund_saga")
public class OrderItemRefundSaga extends OrderSaga {
    @Column(nullable = false)
    Long orderItemId;

    @Setter
    @Enumerated(EnumType.STRING)
    private ItemRefundSagaStep lastCompletedStep;

    private OrderItemRefundSaga(UUID sagaId, Long orderId, Long orderItemId) {
        super(sagaId, orderId);
        this.orderItemId = orderItemId;
        this.lastCompletedStep = ItemRefundSagaStep.STARTED;
    }

    public static OrderItemRefundSaga create(UUID sagaId, Long orderId, Long orderItemId) {
        return new OrderItemRefundSaga(sagaId, orderId, orderItemId);
    }
}
