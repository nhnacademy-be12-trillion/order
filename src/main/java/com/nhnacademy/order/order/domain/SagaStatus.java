package com.nhnacademy.order.order.domain;

public enum SagaStatus {
    PROGRESS,   // 진행 중
    COMPLETED,  // 성공
    FAILED,     // 실패
    COMPENSATED // 보상 트랜잭션 진행 중
}
