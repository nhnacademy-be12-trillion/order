package com.nhnacademy.order.order.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class OrderSaga {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sagaId;

    private Long orderId;

    @Setter
    @Enumerated(EnumType.STRING)
    private SagaStatus overallStatus;   // 전체적인 상태

    @Setter
    @Enumerated(EnumType.STRING)
    private SagaStep lastCompletedStep; // 마지막으로 성공한 단계

    public static OrderSaga create(Long orderId) {
        return new OrderSaga(
            null,
            orderId,
            SagaStatus.PROGRESS,
            SagaStep.STARTED
        );
    }
}
