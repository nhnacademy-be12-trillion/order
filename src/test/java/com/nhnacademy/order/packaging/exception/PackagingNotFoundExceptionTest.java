package com.nhnacademy.order.packaging.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PackagingNotFoundExceptionTest {

    @Test
    @DisplayName("예외 생성 및 메시지 확인 테스트")
    void testExceptionCreation() {
        String errorMessage = "Packaging not found.";
        PackagingNotFoundException exception = new PackagingNotFoundException(errorMessage);

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo(errorMessage);
    }
}
