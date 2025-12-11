package com.nhnacademy.order.ordersaga.cancellation.domain;

import com.nhnacademy.order.ordersaga.domain.OrderSaga;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "order_cancel_saga")
public class OrderCancelSaga extends OrderSaga {
    @Setter
    @Enumerated(EnumType.STRING)
    private CancelSagaStep lastCompletedStep;

    private OrderCancelSaga(UUID sagaId, Long orderId) {
        super(sagaId, orderId);
        this.lastCompletedStep = CancelSagaStep.STARTED;
    }

    public static OrderCancelSaga create(UUID sagaId, Long orderId) {
        return new OrderCancelSaga(sagaId, orderId);
    }
}
