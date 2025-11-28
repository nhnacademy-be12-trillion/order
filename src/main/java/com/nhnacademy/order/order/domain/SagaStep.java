package com.nhnacademy.order.order.domain;

public enum SagaStep {
    STARTED,            // 시작
    STOCK_DECREASED,    // 도서 재고 감소
    COUPON_APPLIED,     // 쿠폰 적용
    POINT_USED,         // 포인트 사용
}
