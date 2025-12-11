package com.nhnacademy.order.ordersaga.creation.domain;

public enum CreateSagaStep {
    STARTED,
    STOCK_DECREASING, // 재고 감소 중
    STOCK_DECREASED,  // 재고 감소 완료
    COUPON_APPLYING,  // 쿠폰 적용 중
    COUPON_APPLIED,   // 쿠폰 적용 완료
    POINT_USING,      // 포인트 사용 중
    POINT_USED        // 포인트 사용 완료
}
