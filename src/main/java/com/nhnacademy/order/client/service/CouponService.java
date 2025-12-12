package com.nhnacademy.order.client.service;

import com.nhnacademy.order.client.CouponClient;
import com.nhnacademy.order.client.dto.CouponApplyRequest;
import com.nhnacademy.order.client.dto.CouponGetDiscountAmountRequest;
import com.nhnacademy.order.client.handler.ResilienceFallbackHandler;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponClient couponClient;
    private final ResilienceFallbackHandler fallbackHandler;
    private static final String SERVICE_NAME = "쿠폰 API";

    @CircuitBreaker(name = "coupon-service", fallbackMethod = "fallbackCalculateDiscount")
    @Retry(name = "coupon-service")
    public int calculateDiscount(Long couponId, int price) {
        return couponClient.calculateDiscount(new CouponGetDiscountAmountRequest(couponId, price));
    }

    @CircuitBreaker(name = "coupon-service", fallbackMethod = "fallbackApplyCoupon")
    @Retry(name = "coupon-service")
    public void applyCoupon(Long memberId, Long couponId) {
        couponClient.applyCoupon(new CouponApplyRequest(memberId, couponId));
    }

    @CircuitBreaker(name = "coupon-service", fallbackMethod = "fallbackWithdrawCoupon")
    @Retry(name = "coupon-service")
    public void withdrawCoupon(Long memberId, Long couponId) {
        couponClient.withdrawCoupon(new CouponApplyRequest(memberId, couponId));
    }

    public int fallbackCalculateDiscount(Long couponId, int price, Throwable throwable) {
        return fallbackHandler.handle(SERVICE_NAME, "쿠폰 할인가 계산", throwable);
    }

    public void fallbackApplyCoupon(Long memberId, Long couponId, Throwable throwable) {
        fallbackHandler.handle(SERVICE_NAME, "쿠폰 사용", throwable);
    }

    public void fallbackWithdrawCoupon(Long memberId, Long couponId, Throwable throwable) {
        fallbackHandler.handle(SERVICE_NAME, "쿠폰 복원", throwable);
    }
}
