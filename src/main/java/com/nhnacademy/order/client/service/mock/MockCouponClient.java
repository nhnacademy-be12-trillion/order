package com.nhnacademy.order.client.service.mock;

import com.nhnacademy.order.client.CouponClient;
import com.nhnacademy.order.client.dto.CouponApplyRequest;
import com.nhnacademy.order.client.dto.CouponGetDiscountAmountRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("local")
@Component
public class MockCouponClient implements CouponClient {

    @Override
    public int calculateDiscount(CouponGetDiscountAmountRequest request) {
        log.info("MockCouponClient calculateDiscount called with: {}", request);
        // Return a mock discount amount, e.g., 1000.
        return 1000;
    }

    @Override
    public void applyCoupon(CouponApplyRequest request) {
        log.info("MockCouponClient applyCoupon called with: {}", request);
        // In a mock, we just log the action. No real state change.
    }

    @Override
    public void withdrawCoupon(CouponApplyRequest request) {
        log.info("MockCouponClient withdrawCoupon called with: {}", request);
        // In a mock, we just log the action. No real state change.
    }
}