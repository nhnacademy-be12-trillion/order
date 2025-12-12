package com.nhnacademy.order.client.service;

import com.nhnacademy.order.client.MemberClient;
import com.nhnacademy.order.client.dto.PointUsageRequest;
import com.nhnacademy.order.client.handler.ResilienceFallbackHandler;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberClient memberClient;
    private final ResilienceFallbackHandler fallbackHandler;
    private static final String SERVICE_NAME = "회원 API";

    @CircuitBreaker(name = "member-service", fallbackMethod = "fallbackDecreasePoint")
    @Retry(name = "member-service")
    public void decreasePoint(Long memberId, int point) {
        memberClient.decreasePoint(new PointUsageRequest(memberId, point));
    }

    @CircuitBreaker(name = "member-service", fallbackMethod = "fallbackIncreasePoint")
    @Retry(name = "member-service")
    public void increasePoint(Long memberId, int point) {
        memberClient.increasePoint(new PointUsageRequest(memberId, point));
    }

    @CircuitBreaker(name = "member-service", fallbackMethod = "fallbackRollbackPoint")
    @Retry(name = "member-service")
    public void rollbackPoint(Long memberId, int point) {
        memberClient.rollbackPoint(new PointUsageRequest(memberId, point));
    }

    public void fallbackDecreasePoint(Long memberId, int point, Throwable throwable) {
        fallbackHandler.handle(SERVICE_NAME, "포인트 감소", throwable);
    }

    public void fallbackIncreasePoint(Long memberId, int point, Throwable throwable) {
        fallbackHandler.handle(SERVICE_NAME, "포인트 증가", throwable);
    }

    public void fallbackRollbackPoint(Long memberId, int point, Throwable throwable) {
        fallbackHandler.handle(SERVICE_NAME, "포인트 복구", throwable);
    }
}
