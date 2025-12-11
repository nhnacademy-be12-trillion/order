package com.nhnacademy.order.ordersaga.creation.domain;

import com.nhnacademy.order.ordersaga.domain.OrderSaga;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "order_create_saga")
public class OrderCreateSaga extends OrderSaga {
    @Setter
    @Enumerated(EnumType.STRING)
    private CreateSagaStep lastCompletedStep; // 마지막으로 성공한 단계

    private OrderCreateSaga(UUID sagaId, Long orderId) {
        super(sagaId, orderId);
        this.lastCompletedStep = CreateSagaStep.STARTED;
    }

    public static OrderCreateSaga create(UUID sagaId, Long orderId) {
        return new OrderCreateSaga(sagaId, orderId);
    }
}
