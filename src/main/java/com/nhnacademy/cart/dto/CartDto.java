package com.nhnacademy.cart.dto;

import com.nhnacademy.cart.domain.EntityCart;
import com.nhnacademy.cart.domain.RedisCart;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDto {
    private Long bookId;
    private int cartQuantity;
    private LocalDateTime createdAt;

    // Redis -> DTO 변환
    public static CartDto fromRedis(RedisCart redisCart) {
        return CartDto.builder()
                .bookId(redisCart.getBookId())
                .cartQuantity(redisCart.getCartQuantity())
                .createdAt(LocalDateTime.parse(redisCart.getCreatedAt()))
                .build();
    }

    // BD -> DTO 변환
    public static CartDto fromEntity(EntityCart entityCart) {
        return CartDto.builder()
                .bookId(entityCart.getBookId())
                .cartQuantity(entityCart.getCartQuantity())
                .createdAt(entityCart.getCreatedAt())
                .build();
    }
}