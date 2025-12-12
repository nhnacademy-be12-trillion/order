package com.nhnacademy.cart.controller;

import com.nhnacademy.cart.dto.CartHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Cart API", description = "장바구니 관련 API (담기, 조회, 수정, 삭제, 병합)")
public interface CartControllerDocs {

    // =================================================================================
    // [공통 설명]
    // 모든 API는 'X-User-Id'(회원) 헤더 또는 'CART_SESSION'(비회원) 쿠키 중 하나가 필수입니다.
    // =================================================================================

    @Operation(summary = "장바구니 담기", description = "상품을 장바구니에 추가합니다. 이미 존재하면 수량을 합산합니다.")
    @Parameters({
            @Parameter(name = "X-User-Id", description = "회원 식별 ID (회원인 경우 입력)", in = ParameterIn.HEADER, example = "1"),
            @Parameter(name = "SESSION", description = "비회원 세션 ID (비회원인 경우 입력)", in = ParameterIn.COOKIE, example = "guest-1234")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "성공 (현재 장바구니 총 아이템 개수 반환)",
                    content = @Content(schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 수량 등)", content = @Content),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 도서 ID", content = @Content)
    })
    ResponseEntity<Long> addCartItem(
            @Parameter(hidden = true) CartHolder holder, // 내부 주입용이므로 숨김
            @RequestBody CartCreateRequestDto request
    );

    @Operation(summary = "수량 변경", description = "장바구니에 담긴 특정 도서의 수량을 변경합니다.")
    @Parameters({
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, example = "1"),
            @Parameter(name = "SESSION", in = ParameterIn.COOKIE)
    })
    @ApiResponse(responseCode = "200", description = "수정 성공")
    ResponseEntity<Void> updateCartItem(
            @Parameter(hidden = true) CartHolder holder,
            @Parameter(description = "도서 ID", example = "101") @PathVariable Long bookId,
            @RequestBody CartUpdateRequestDto request
    );

    @Operation(summary = "상품 삭제", description = "장바구니에서 특정 도서를 삭제합니다.")
    @Parameters({
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, example = "1"),
            @Parameter(name = "SESSION", in = ParameterIn.COOKIE)
    })
    ResponseEntity<Void> removeCartItem(
            @Parameter(hidden = true) CartHolder holder,
            @Parameter(description = "삭제할 도서 ID") @PathVariable Long bookId
    );

    @Operation(summary = "장바구니 비우기", description = "장바구니의 모든 상품을 삭제합니다.")
    @Parameters({
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, example = "1"),
            @Parameter(name = "SESSION", in = ParameterIn.COOKIE)
    })
    ResponseEntity<Void> clearCart(
            @Parameter(hidden = true) CartHolder holder
    );

    @Operation(summary = "장바구니 목록 조회", description = "현재 장바구니에 담긴 모든 상품 목록을 최신순으로 반환합니다.")
    @Parameters({
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, example = "1"),
            @Parameter(name = "SESSION", in = ParameterIn.COOKIE)
    })
    ResponseEntity<List<CartResponseDto>> getCartItems(
            @Parameter(hidden = true) CartHolder holder
    );

    @Operation(summary = "장바구니 개수 조회", description = "헤더 아이콘 배지 표시용 총 개수를 반환합니다.")
    @Parameters({
            @Parameter(name = "X-User-Id", in = ParameterIn.HEADER, example = "1"),
            @Parameter(name = "SESSION", in = ParameterIn.COOKIE)
    })
    ResponseEntity<Long> countCartItems(
            @Parameter(hidden = true) CartHolder holder
    );

    @Operation(summary = "장바구니 병합", description = "로그인 시, 비회원 장바구니(Cookie)의 내용을 회원 장바구니(Header)로 옮깁니다.")
    @Parameters({
            @Parameter(name = "X-User-Id", description = "병합될 회원 ID (필수)", in = ParameterIn.HEADER, required = true, example = "1"),
            @Parameter(name = "SESSION", description = "가져올 비회원 세션 ID (필수)", in = ParameterIn.COOKIE, required = true, example = "guest-1234")
    })
    ResponseEntity<Void> mergeCart(
            @Parameter(hidden = true) CartHolder memberHolder,
            @Parameter(hidden = true) CartHolder guestHolder
    );
}