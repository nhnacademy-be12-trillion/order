package com.nhnacademy.order.order.exception;

public class OrderCreateFailureException extends RuntimeException {
    public OrderCreateFailureException(String message) {
        super(message);
    }
}
