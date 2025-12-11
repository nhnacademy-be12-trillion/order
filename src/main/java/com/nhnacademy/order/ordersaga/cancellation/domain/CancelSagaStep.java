package com.nhnacademy.order.ordersaga.cancellation.domain;

import lombok.Getter;

@Getter
public enum CancelSagaStep {
    STARTED,            // 시작
    PAYMENT_CANCELED,   // 결제 취소
    POINT_REFUNDED,     // 포인트 반환
    COUPON_RESTORED,    // 쿠폰 반환
    STOCK_INCREASED     // 재고 증가
}
