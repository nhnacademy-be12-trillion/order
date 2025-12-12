package com.nhnacademy.cart.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.nhnacademy.cart.dto.CartDto;
import com.nhnacademy.cart.dto.CartHolder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) //널값 필드는 JSON에서 제외
class CartResponseDto {

    private Long memberId;   // 회원일 때만 값 있음
    private String guestId;  // 비회원일 때만 값 있음

    private Long bookId;
    private int cartQuantity;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt;

    public static CartResponseDto of(CartDto cartDto, CartHolder holder) {
        return CartResponseDto.builder()
                // 회원 정보 매핑 (회원이면 memberId, 아니면 guestId)
                .memberId(holder.isMember() ? holder.getMemberId() : null)
                .guestId(holder.isMember() ? null : holder.getGuestId())

                // 상품 정보 매핑
                .bookId(cartDto.getBookId())
                .cartQuantity(cartDto.getCartQuantity())
                .createdAt(cartDto.getCreatedAt())
                .build();
    }
}