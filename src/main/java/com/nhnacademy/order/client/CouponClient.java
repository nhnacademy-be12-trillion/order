package com.nhnacademy.order.client;

import com.nhnacademy.order.client.dto.CouponApplyRequest;
import com.nhnacademy.order.client.dto.CouponGetDiscountAmountRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "coupon-service")
public interface CouponClient {
    @GetMapping("/api/coupon/1")
    int calculateDiscount(@RequestBody CouponGetDiscountAmountRequest request);

    @PostMapping("/api/coupon/2")
    void applyCoupon(@RequestBody CouponApplyRequest request);

    @PostMapping("/api/coupon/3")
    void withdrawCoupon(@RequestBody CouponApplyRequest request);
}
