package com.nhnacademy.cart.controller;

import com.nhnacademy.cart.dto.CartDto;
import com.nhnacademy.cart.dto.CartHolder;
import com.nhnacademy.cart.dto.CartSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CartSummaryResponseDto {
    private long lineCount;      // 종류 수
    private long totalQuantity;  // 전체 수량

    public static CartSummaryResponseDto of(CartSummaryDto dto) {
        return CartSummaryResponseDto.builder()
                .lineCount(dto.getLineCount())
                .totalQuantity(dto.getTotalQuantity())
                .build();
    }
}
