package com.nhnacademy.order.common.handler;

import com.nhnacademy.order.common.dto.ErrorResponse;
import com.nhnacademy.order.order.exception.OrderNotFoundException;
import com.nhnacademy.order.order.exception.OrderPasswordMismatchException;
import com.nhnacademy.order.order.exception.OrderStatusTransitionException;
import com.nhnacademy.order.packaging.exception.PackagingNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({
        OrderNotFoundException.class,
        PackagingNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFoundException(Exception ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex.getMessage(), "NOT_FOUND");

        return ResponseEntity.status(404).body(errorResponse);
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class
    })
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        ErrorResponse errorResponse = ErrorResponse.create(errorMessage, "VALIDATION_FAILED");

        return ResponseEntity.status(400).body(errorResponse);
    }

    @ExceptionHandler({
        AccessDeniedException.class
    })
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex.getMessage(), "FORBIDDEN");

        return ResponseEntity.status(403).body(errorResponse);
    }

    @ExceptionHandler({
        OrderPasswordMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handlePasswordMismatchException(OrderPasswordMismatchException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex.getMessage(), "PASSWORD_MISMATCH");
        return ResponseEntity.status(401).body(errorResponse);
    }

    @ExceptionHandler({
        OrderStatusTransitionException.class
    })
    public ResponseEntity<ErrorResponse> handleStatusTransitionException(OrderStatusTransitionException ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex.getMessage(), "INVALID_STATUS_TRANSITION");
        return ResponseEntity.status(409).body(errorResponse);
    }

    @ExceptionHandler({
        Exception.class
    })
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = ErrorResponse.create(ex.getMessage(), "INTERNAL_SERVER_ERROR");
        return ResponseEntity.status(500).body(errorResponse);
    }
}
