package com.nhnacademy.cart.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE) // 가장 높은 우선순위 부여 (다른 전역 핸들러보다 먼저 실행)
@RestControllerAdvice(basePackages = "com.nhnacademy.cart") // Cart 패키지 내부의 컨트롤러에서 발생한 에러만 처리
public class CartExceptionHandler {

    // 404 Not Found (장바구니 없음 등 도메인 에러)
    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<CartErrorResponse> handleCartNotFoundException(CartNotFoundException e, HttpServletRequest request) {
        log.warn("CartNotFoundException : {}", e.getMessage(), e); // 스택트레이스는 필요할 때만 찍도록 간소화 가능
        CartErrorResponse cartErrorResponse = new CartErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                e.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(cartErrorResponse, HttpStatus.NOT_FOUND);
    }

    // 409 Conflict (용량 초과 등 도메인 로직 충돌)
    @ExceptionHandler(CartCapacityExceededException.class)
    public ResponseEntity<CartErrorResponse> handleCartCapacityExceededException(CartCapacityExceededException e, HttpServletRequest request) {
        log.warn("CartCapacityExceededException: {}", e.getMessage(), e);
        CartErrorResponse cartErrorResponse = new CartErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                e.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(cartErrorResponse, HttpStatus.CONFLICT);
    }

    // 400 Bad Request (@Valid 유효성 검사 실패)
    // 이 핸들러는 'Cart 패키지' 내의 DTO 검증 실패만 잡습니다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CartErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Cart Validation failed : {}", errorMessage);
        CartErrorResponse cartErrorResponse = new CartErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed: " + errorMessage,
                request.getRequestURI()
        );
        return new ResponseEntity<>(cartErrorResponse, HttpStatus.BAD_REQUEST);
    }

    // 400 Bad Request (날짜 파싱 오류)
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<CartErrorResponse> handleDateTimeParseException(DateTimeParseException e, HttpServletRequest request) {
        log.warn("DateTimeParseException inside Cart : {}", e.getMessage(), e);
        CartErrorResponse cartErrorResponse = new CartErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Date/Time format error: " + e.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(cartErrorResponse, HttpStatus.BAD_REQUEST);
    }

    // 400 Bad Request (일반적인 잘못된 인자)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CartErrorResponse> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("IllegalArgumentException inside Cart : {}", e.getMessage(), e);
        CartErrorResponse cartErrorResponse = new CartErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                e.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(cartErrorResponse, HttpStatus.BAD_REQUEST);
    }

    // 500 Internal Server Error (그 외 잡히지 않은 모든 에러)
    // basePackages 설정 덕분에 'Cart 기능 수행 중' 발생한 알 수 없는 에러만 여기서 잡습니다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CartErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled Exception in Cart Service : {}", e.getMessage(), e); // 500 에러는 스택트레이스 필수
        CartErrorResponse cartErrorResponse = new CartErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Cart Internal Server Error: " + e.getMessage(), // 클라이언트에게 너무 구체적인 정보를 숨길지 결정 필요
                request.getRequestURI()
        );
        return new ResponseEntity<>(cartErrorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}