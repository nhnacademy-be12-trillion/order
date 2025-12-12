package com.nhnacademy.cart.dto;

import lombok.Getter;

/**
 * arameter Object 패턴을 적용
 * 장바구니의 주체(회원 or 비회원)를 명확히 식별하기 위함...
 */
@Getter
public class CartHolder {
    private final Long memberId;
    private final String guestId;

    private CartHolder(Long memberId, String guestId) {
        this.memberId = memberId;
        this.guestId = guestId;
    }

    public static CartHolder member(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("MemberId must not be null");
        }
        return new CartHolder(memberId, null);
    }

    public static CartHolder guest(String guestId) {
        if (guestId == null) {
            throw new IllegalArgumentException("GuestId must not be null");
        }
        return new CartHolder(null, guestId);
    }

    public boolean isMember() {
        return memberId != null;
    }
}