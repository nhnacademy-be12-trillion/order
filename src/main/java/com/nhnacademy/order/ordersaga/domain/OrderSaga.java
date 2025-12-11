package com.nhnacademy.order.ordersaga.domain;

import com.nhnacademy.order.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class OrderSaga extends BaseTimeEntity {
    // MySQL은 기본적으로 UUID를 하이픈을 포함한 36자 문자열로 간주하여 VARCHAR(255) 또는 이와 유사한 타입으로 저장하려 함
    // UUID를 가장 효율적으로 저장하고 검색 성능을 최적화하기 위해 16바이트의 이진 데이터로 저장하도록 강제
    @Id
    // @GeneratedValue(strategy = GenerationType.UUID) -> DB에서 생성하는 것이 아닌 애플리케이션에서 생성
    // TODO: H2에서 호환 안됨 -> MySQL 사용할 때 변경
    // @Column(columnDefinition = "BINARY(16)")
    private UUID sagaId;

    @Column(nullable = false)
    private Long orderId;

    @Setter
    @Enumerated(EnumType.STRING)
    private SagaStatus overallStatus;

    @Setter
    @Column(nullable = false)
    private boolean bridged = false;

    protected OrderSaga(UUID sagaId, Long orderId) {
        this.sagaId = sagaId;
        this.orderId = orderId;
        this.overallStatus = SagaStatus.PROGRESS;
    }

    protected OrderSaga() {}
}
