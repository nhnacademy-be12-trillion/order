package com.nhnacademy.order.delivery.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyNotConfiguredExceptionTest {

    @Test
    @DisplayName("예외 생성 및 메시지 확인 테스트")
    void testExceptionCreation() {
        String errorMessage = "Delivery policy is not configured.";
        PolicyNotConfiguredException exception = new PolicyNotConfiguredException(errorMessage);

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
}
