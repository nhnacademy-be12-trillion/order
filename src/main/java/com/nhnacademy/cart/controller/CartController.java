package com.nhnacademy.cart.controller;

import com.nhnacademy.cart.common.annotation.GuestOnly;
import com.nhnacademy.cart.dto.CartDto;
import com.nhnacademy.cart.dto.CartHolder;
import com.nhnacademy.cart.dto.CartSummaryDto;
import com.nhnacademy.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController implements CartControllerDocs {

    private final CartService cartService;

    /**
     * [장바구니 담기]
     * POST /api/carts
     * - CartHolder: Resolver가 헤더(회원)/쿠키(비회원)를 판단해 주입
     * - Response: 담기 성공 후 UI 갱신을 위해 '현재 장바구니 총 개수'를 반환
     */
    @PostMapping
    public ResponseEntity<Void> addCartItem(
            CartHolder holder,
            @Valid @RequestBody CartCreateRequestDto request
    ) {
        // Request DTO -> Service DTO 변환
        CartDto serviceDto = CartDto.builder()
                .bookId(request.getBookId())
                .cartQuantity(request.getCartQuantity())
                .build();

        cartService.addCartItem(holder, serviceDto);

        return ResponseEntity.noContent().build();
    }

    /**
     * [수량 변경]
     * PUT /api/carts/{bookId}
     * - Body: { "quantity": 5 } 형태의 JSON
     */
    @PutMapping("/{bookId}")
    public ResponseEntity<Void> updateCartItem(
            CartHolder holder,
            @PathVariable Long bookId,
            @Valid @RequestBody CartUpdateRequestDto request
    ) {
        cartService.updateCartItem(holder, bookId, request.getCartQuantity());
        return ResponseEntity.noContent().build();
    }

    /**
     * [상품 삭제]
     * DELETE /api/carts/{bookId}
     */
    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeCartItem(CartHolder holder, @PathVariable Long bookId) {
        cartService.removeCartItem(holder, bookId);
        return ResponseEntity.noContent().build();
    }

    /**
     * [장바구니 비우기]
     * DELETE /api/carts
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart(CartHolder holder) {
        cartService.clearCart(holder);
        return ResponseEntity.noContent().build();
    }

    /**
     * [장바구니 목록 조회]
     * GET /api/carts
     */
    @GetMapping
    public ResponseEntity<List<CartResponseDto>> getCartItems(CartHolder holder) {
        List<CartDto> items = cartService.getCartItems(holder);

        List<CartResponseDto> responses = items.stream()
                .sorted(Comparator.comparing(CartDto::getCreatedAt).reversed())
                .map(dto -> CartResponseDto.of(dto, holder))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * [장바구니 요약 정보 조회]
     * GET /api/carts/summary
     * - 헤더 아이콘 배지 표시용
     */
    @GetMapping("/summary")
    public ResponseEntity<CartSummaryResponseDto> getCartSummary(CartHolder holder) {
        CartSummaryDto cartSummaryDto = cartService.getCartSummary(holder);

        return ResponseEntity.ok(CartSummaryResponseDto.of(cartSummaryDto));
    }

    /**
     * [장바구니 병합 (로그인 직후)]
     * POST /api/carts/merge
     * - memberHolder: 헤더(X-User-Id)에서 파싱된 회원 정보
     * - guestHolder: 쿠키(guestId)에서 강제로 파싱된 비회원 정보 (@GuestOnly)
     */
    @PostMapping("/merge")
    public ResponseEntity<Void> mergeCart(
            CartHolder memberHolder,           // Resolver가 헤더 보고 주입
            @GuestOnly CartHolder guestHolder  // Resolver가 쿠키만 보고 주입
    ) {
        cartService.mergeCart(memberHolder, guestHolder);
        return ResponseEntity.noContent().build();
    }
}