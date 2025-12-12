package com.nhnacademy.cart.common.exception;

public class InvalidCartQuantityException extends RuntimeException {
    public InvalidCartQuantityException(String message) {
        super(message);
    }
}
