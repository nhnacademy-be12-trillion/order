package com.nhnacademy.cart.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CartLoggingAspect {

    @Pointcut("execution(public * com.nhnacademy.cart..service.*.*(..))")
    public void serviceLayerExecution() {}

    @Around("serviceLayerExecution()")
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();

        // 시간 측정을 위해 사용...
        StopWatch stopWatch = new StopWatch();

        log.info("---> [시작] {}", methodName);
        stopWatch.start();

        try {
            // 이 proceed() 내부에서 @Transactional AOP가 실행됨
            // 즉, 트랜잭션 시작 -> 서비스 로직 -> 트랜잭션 커밋(Flush) -> 리턴 순으로 진행
            Object result = joinPoint.proceed();
            return result;
        } finally {
            stopWatch.stop();

            // 트랜잭션 커밋(Dirty Checking UPDATE 쿼리)까지 모두 포함된 시간
            log.info("<--- [끝] {} (실행 시간: {} ms)", methodName, stopWatch.getTotalTimeMillis());
        }
    }
}