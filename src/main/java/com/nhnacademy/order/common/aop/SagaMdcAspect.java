/*
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 * + Copyright 2025. NHN Academy Corp. All rights reserved.
 * + * While every precaution has been taken in the preparation of this resource,  assumes no
 * + responsibility for errors or omissions, or for damages resulting from the use of the information
 * + contained herein
 * + No part of this resource may be reproduced, stored in a retrieval system, or transmitted, in any
 * + form or by any means, electronic, mechanical, photocopying, recording, or otherwise, without the
 * + prior written permission.
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 */

package com.nhnacademy.order.common.aop;

import com.nhnacademy.order.common.context.SagaContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Aspect
@Component
public class SagaMdcAspect {
    private static final String SAGA_ID_MDC_KEY = "sagaId";

    @Around("@annotation(com.nhnacademy.order.common.aop.SagaIdContext)")
    public Object startSagaToMdc(ProceedingJoinPoint joinPoint) throws Throwable {

        UUID sagaId = UUID.randomUUID();

        try {
            MDC.put(SAGA_ID_MDC_KEY, sagaId.toString());
            SagaContext.set(sagaId);

            log.debug("사가 MDC 설정 - sagaId={}", sagaId);

            return joinPoint.proceed();
        } finally {
            MDC.remove(SAGA_ID_MDC_KEY);
            SagaContext.clear();

            log.debug("사가 MDC 종료 - sagaId={}", sagaId);
        }
    }
}
