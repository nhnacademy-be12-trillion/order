package com.nhnacademy.order.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {
    @Pointcut("execution(public * com.nhnacademy.order..service.*.*(..)) ||" +
            "execution(public * com.nhnacademy.order..ordersaga..*.*(..))")
    public void serviceLayerExecution() {}

    @Around("serviceLayerExecution()")
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        log.info("---> [시작] {}", methodName);

        long startTime = System.currentTimeMillis();

        try {
            return joinPoint.proceed();
        } finally {
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            // JPA의 변경 감지로 인한 UPDATE 쿼리가 해당 로그가 출력된 후 DB로 전송됨
            // 1. @TransactionalEventListener로 해결? -> AI가 권장 (트랜잭션 최종 커밋 이후 -> 트랜잭션 성공 보장, 트랜잭션 시점과 로깅 시점을 정확히 일치)
            // 2. Service에서 EntityManager를 주입받아 수동 flush() -> 간단함 (flush() 이후 트랜잭션 롤백 가능성 -> 트랜잭션 성공 보장 X)
            log.info("<--- [끝] {} (실행 시간: {} ms)", methodName, executionTime);
        }
    }
}
