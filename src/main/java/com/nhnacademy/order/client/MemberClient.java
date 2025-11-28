package com.nhnacademy.order.client;

import com.nhnacademy.order.client.dto.PointUsageRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "member-service")
public interface MemberClient {
    @PatchMapping("/api/point/1")
    void increasePoint(@RequestBody PointUsageRequest request);

    @PatchMapping("/api/point/2")
    void decreasePoint(@RequestBody PointUsageRequest request);
}
