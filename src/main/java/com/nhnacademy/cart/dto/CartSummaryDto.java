package com.nhnacademy.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartSummaryDto {
     private long lineCount;      // 종류 수
     private long totalQuantity;  // 전체 수량
}