package com.nhnacademy.cart.service;

import com.nhnacademy.cart.dto.CartDto;
import com.nhnacademy.cart.dto.CartHolder;
import com.nhnacademy.cart.dto.CartSummaryDto;

import java.util.List;

/**
 * 장바구니 비즈니스 로직 서비스 인터페이스.
 */
public interface CartService {

    /**
     * [상품 추가]
     * - 장바구니 아이템 하나를 추가 (있으면 합산)
     */
    void addCartItem(CartHolder holder, CartDto cartDto);

    /**
     * [수량 변경]
     * - 장바구니 아이템(의 수량)을 갱신
     */
    void updateCartItem(CartHolder holder, Long bookId, Integer quantity);

    /**
     * [상품 삭제]
     * -장바구니 아이템 하나를 제거
     */
    void removeCartItem(CartHolder holder, Long bookId);

    /**
     * [상품 목록 조회]
     * - 장바구니 아이템들의 목록을 가져옴
     */
    List<CartDto> getCartItems(CartHolder holder);

    /**
     * [장바구니 요약 정보 조회]
     * - 상품의 종류(Line) 수와 전체 수량(Total Quantity)을 포함한 요약 정보를 반환합니다.
     */
    CartSummaryDto getCartSummary(CartHolder holder);

    // --------------------------------------------------------

    /**
     * [장바구니 비우기]
     * - 카트 전체를 비움
     */
    void clearCart(CartHolder holder);

    /**
     * [장바구니 병합]
     * - (비회원) 카트를 (회원) 카트와 병합
     */
    void mergeCart(CartHolder memberHolder, CartHolder guestHolder);
}