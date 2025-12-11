package com.nhnacademy.order.order.domain;

public enum OrderStatus {
    PENDING,    // 결제 대기
    COMPLETED,  // 결제 완료
    CANCELED,   // 모든 상품 취소/환불

    // 사가를 위한 상태
    CREATING,           // 주문 생성 사가 진행 중
    CREATION_FAILED,    // 주문 생성 실패
    CANCELING           // 주문 취소 사가 진행 중
}
