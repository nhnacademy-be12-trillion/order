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

package com.nhnacademy.order.client.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class FeignSagaIdInterceptor implements RequestInterceptor {
    public static final String SAGA_ID_HEADER = "X-SAGA-ID";
    public static final String SAGA_ID_MDC_KEY = "sagaId";

    // 요청 헤더에 사가 ID 부착
    @Override
    public void apply(RequestTemplate requestTemplate) {
        String sagaId = MDC.get(SAGA_ID_MDC_KEY);

        if (sagaId != null && !sagaId.isEmpty()) {
            requestTemplate.header(SAGA_ID_HEADER, sagaId);
        }
    }
}
