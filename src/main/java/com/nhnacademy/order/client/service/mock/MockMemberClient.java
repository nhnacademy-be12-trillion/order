package com.nhnacademy.order.client.service.mock;

import com.nhnacademy.order.client.MemberClient;
import com.nhnacademy.order.client.dto.PointUsageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("local")
@Component
public class MockMemberClient implements MemberClient {

    @Override
    public void increasePoint(PointUsageRequest request) {
        log.info("MockMemberClient increasePoint called with: {}", request);
        // In a mock, we just log the action. No real state change.
    }

    @Override
    public void decreasePoint(PointUsageRequest request) {
        log.info("MockMemberClient decreasePoint called with: {}", request);
        // In a mock, we just log the action. No real state change.
    }

    @Override
    public void rollbackPoint(PointUsageRequest request) {
        log.info("MockMemberClient rollbackPoint called with: {}", request);
        // In a mock, we just log the action. No real state change.
    }
}