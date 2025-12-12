package com.nhnacademy.cart.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RedisCart implements Serializable {
    private Long bookId;
    private int cartQuantity;
    private String createdAt;

    public static RedisCart create(Long bookId, Integer cartQuantity, LocalDateTime createdAt) {
        return RedisCart.builder()
                .bookId(bookId)
                .cartQuantity(cartQuantity)
                .createdAt(createdAt.toString())
                .build();
    }
    public void updateQuantity(int cartQuantity){
        this.cartQuantity=cartQuantity;
    }
}