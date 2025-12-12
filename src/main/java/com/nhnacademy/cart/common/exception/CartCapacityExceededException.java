package com.nhnacademy.cart.common.exception;

public class CartCapacityExceededException extends RuntimeException {
    public CartCapacityExceededException(String message) {
        super(message);
    }
}
