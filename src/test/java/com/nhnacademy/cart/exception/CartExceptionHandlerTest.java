package com.nhnacademy.cart.exception;

import com.nhnacademy.cart.common.exception.CartCapacityExceededException;
import com.nhnacademy.cart.common.exception.CartErrorResponse;
import com.nhnacademy.cart.common.exception.CartExceptionHandler;
import com.nhnacademy.cart.common.exception.CartNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.format.DateTimeParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartExceptionHandlerTest {
    @InjectMocks
    private CartExceptionHandler cartExceptionHandler;

    @Mock
    private HttpServletRequest request;

    private static final String TEST_URI = "/api/test";

    @BeforeEach
    void setUp() {
        // 모든 테스트 공통: 요청 URI 모킹
        lenient().when(request.getRequestURI()).thenReturn(TEST_URI);
    }

    @Test
    @DisplayName("404 Handle CartNotFoundException")
    void handleCartNotFoundException() {
        // given
        String errorMessage = "장바구니를 찾을 수 없습니다.";
        CartNotFoundException ex = new CartNotFoundException(errorMessage);

        // when
        ResponseEntity<CartErrorResponse> response = cartExceptionHandler.handleCartNotFoundException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
        assertThat(response.getBody().getPath()).isEqualTo(TEST_URI);
    }

    @Test
    @DisplayName("409 Handle CartCapacityExceededException")
    void handleCartCapacityExceededException() {
        // given
        String errorMessage = "장바구니 용량 초과";
        CartCapacityExceededException ex = new CartCapacityExceededException(errorMessage);

        // when
        ResponseEntity<CartErrorResponse> response = cartExceptionHandler.handleCartCapacityExceededException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("400 Handle MethodArgumentNotValidException (Validation 실패)")
    void handleMethodArgumentNotValidException() {
        // given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("cartDto", "quantity", "must be greater than 0");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        // when
        ResponseEntity<CartErrorResponse> response = cartExceptionHandler.handleMethodArgumentNotValidException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("Validation failed");
        assertThat(response.getBody().getMessage()).contains("quantity: must be greater than 0");
    }

    @Test
    @DisplayName("400 Handle DateTimeParseException (날짜 파싱 오류)")
    void handleDateTimeParseException() {
        // given
        String invalidDate = "2025-13-40";
        DateTimeParseException ex = new DateTimeParseException("Text could not be parsed", invalidDate, 0);

        // when
        ResponseEntity<CartErrorResponse> response = cartExceptionHandler.handleDateTimeParseException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("Date/Time format error");
    }

    @Test
    @DisplayName("400 Handle IllegalArgumentException")
    void handleIllegalArgumentException() {
        // given
        String errorMessage = "잘못된 인자입니다.";
        IllegalArgumentException ex = new IllegalArgumentException(errorMessage);

        // when
        ResponseEntity<CartErrorResponse> response = cartExceptionHandler.handleIllegalArgumentException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("500 Handle Exception (그 외 모든 예외)")
    void handleException() {
        // given
        String errorMessage = "알 수 없는 서버 오류";
        RuntimeException ex = new RuntimeException(errorMessage);

        // when
        ResponseEntity<CartErrorResponse> response = cartExceptionHandler.handleException(ex, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("Cart Internal Server Error: " + errorMessage);
    }

    @Test
    @DisplayName("ErrorResponse DTO Getter/Setter Test (Lombok 커버리지)")
    void errorResponseDtoTest() {
        // given
        CartErrorResponse errorResponse = new CartErrorResponse();

        // when
        errorResponse.setStatus(404);
        errorResponse.setError("Error");
        errorResponse.setMessage("Message");
        errorResponse.setPath("/path");

        // then
        assertThat(errorResponse.getStatus()).isEqualTo(404);
        assertThat(errorResponse.getError()).isEqualTo("Error");
        assertThat(errorResponse.getMessage()).isEqualTo("Message");
        assertThat(errorResponse.getPath()).isEqualTo("/path");

        // AllArgsConstructor 테스트
        CartErrorResponse allArgs = new CartErrorResponse(500, "Err", "Msg", "/p");
        assertThat(allArgs.getStatus()).isEqualTo(500);
    }
}