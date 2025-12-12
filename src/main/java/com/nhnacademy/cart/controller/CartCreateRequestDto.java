package com.nhnacademy.cart.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
class CartCreateRequestDto {

    @NotNull(message = "도서 ID는 필수입니다.")
    private Long bookId;

    @Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다.")
    private Integer cartQuantity;
}