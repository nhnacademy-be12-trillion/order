package com.nhnacademy.order.client;

import com.nhnacademy.order.client.dto.CouponApplyRequest;
import com.nhnacademy.order.client.dto.CouponGetDiscountAmountRequest;
import com.nhnacademy.order.common.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "coupon-service", configuration = FeignClientConfig.class)
public interface CouponClient {
    @PostMapping("/api/coupon/1")
    int calculateDiscount(@RequestBody CouponGetDiscountAmountRequest request);

    @PostMapping("/api/coupon/2")
    void applyCoupon(@RequestBody CouponApplyRequest request);

    @PostMapping("/api/coupon/3")
    void withdrawCoupon(@RequestBody CouponApplyRequest request);
}
