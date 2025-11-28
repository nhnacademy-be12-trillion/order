package com.nhnacademy.order.order.domain;

public enum PaymentStatus {
    PENDING,    // 결제 대기
    COMPLETED,  // 결제 완료
    CANCELED    // 모든 상품 취소/환불
}
