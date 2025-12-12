package com.nhnacademy.cart.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
}