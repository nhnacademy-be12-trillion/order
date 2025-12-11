package com.nhnacademy.order.client.handler;

import com.nhnacademy.order.client.exception.ExternalServiceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResilienceFallbackHandler {
    public <T> T handle(String serviceName, String operationName, Throwable throwable) {
        if (throwable instanceof CallNotPermittedException) {
            // 서킷이 열려 있는 상태
            log.warn("[{}] 서킷이 열려 있어 요청이 차단됨 - {}", serviceName, operationName);
        } else {
            // fallback이 발생한 상태
            log.error("[{}] 요청 실패 - {}", serviceName, operationName);
        }

        throw new ExternalServiceException(serviceName + " 서비스 오류: " + operationName, throwable);
    }
}
