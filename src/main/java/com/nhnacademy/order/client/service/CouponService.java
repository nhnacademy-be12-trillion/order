package com.nhnacademy.order.client.service;

import com.nhnacademy.order.client.CouponClient;
import com.nhnacademy.order.client.dto.CouponApplyRequest;
import com.nhnacademy.order.client.dto.CouponGetDiscountAmountRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponClient couponClient;

    public int calculateDiscount(Long couponId, int price) {
        return couponClient.calculateDiscount(new CouponGetDiscountAmountRequest(couponId, price));
    }

    public void applyCoupon(Long sagaId, Long memberId, Long couponId) {
        couponClient.applyCoupon(new CouponApplyRequest(sagaId, memberId, couponId));
    }

    public void withdrawCoupon(Long sagaId, Long memberId, Long couponId) {
        couponClient.withdrawCoupon(new CouponApplyRequest(sagaId, memberId, couponId));
    }
}
