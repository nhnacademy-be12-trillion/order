package com.nhnacademy.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.cart.common.annotation.GuestOnly;
import com.nhnacademy.cart.dto.CartDto;
import com.nhnacademy.cart.dto.CartHolder;
import com.nhnacademy.cart.service.CartService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @InjectMocks
    private CartController cartController;

    @Mock
    private CartService cartService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    // Test Fixtures
    private CartHolder memberHolder;
    private CartHolder guestHolder;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        memberHolder = CartHolder.member(1L);
        guestHolder = CartHolder.guest("guest-123");

        // [핵심] ArgumentResolver 모킹 (CartHolder 주입용)
        // Controller가 ArgumentResolver에 의존하므로, 테스트용 Resolver를 직접 만들어 주입
        HandlerMethodArgumentResolver cartHolderResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return CartHolder.class.isAssignableFrom(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                // @GuestOnly 어노테이션이 있으면 GuestHolder 반환
                if (parameter.hasParameterAnnotation(GuestOnly.class)) {
                    return guestHolder;
                }
                // 없으면 MemberHolder 반환 (기본값)
                return memberHolder;
            }
        };

        // StandaloneSetup을 사용하여 컨트롤러만 가볍게 테스트
        mockMvc = MockMvcBuilders.standaloneSetup(cartController)
                .setCustomArgumentResolvers(cartHolderResolver) // Resolver 등록
                .build();
    }

    // ======================================================================
    //  DTO Definitions (테스트를 위한 가짜 DTO 정의)
    // ======================================================================
    @Getter @NoArgsConstructor @AllArgsConstructor
    static class CartCreateRequestDto {
        private Long bookId;
        private Integer cartQuantity;
    }

    @Getter @NoArgsConstructor @AllArgsConstructor
    static class CartUpdateRequestDto {
        private Integer cartQuantity;
    }

    // ======================================================================
    //  Tests
    // ======================================================================

    @Test
    @DisplayName("POST /api/carts : 장바구니 담기 성공 (201 Created)")
    void addCartItem() throws Exception {
        // given
        CartCreateRequestDto request = new CartCreateRequestDto(100L, 2);
        given(cartService.countCartItems(any(CartHolder.class))).willReturn(5L);

        // when & then
        mockMvc.perform(post("/api/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("5")); // 총 개수 반환 확인

        // Verify: Service DTO 변환 및 호출 검증
        verify(cartService).addCartItem(eq(memberHolder), any(CartDto.class));
    }

    @Test
    @DisplayName("PUT /api/carts/{bookId} : 수량 변경 성공 (200 OK)")
    void updateCartItem() throws Exception {
        // given
        Long bookId = 100L;
        CartUpdateRequestDto request = new CartUpdateRequestDto(5);

        // when & then
        mockMvc.perform(put("/api/carts/{bookId}", bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(cartService).updateCartItem(memberHolder, bookId, 5);
    }

    @Test
    @DisplayName("DELETE /api/carts/{bookId} : 상품 삭제 성공 (200 OK)")
    void removeCartItem() throws Exception {
        // given
        Long bookId = 100L;

        // when & then
        mockMvc.perform(delete("/api/carts/{bookId}", bookId))
                .andExpect(status().isNoContent());

        verify(cartService).removeCartItem(memberHolder, bookId);
    }

    @Test
    @DisplayName("DELETE /api/carts : 장바구니 비우기 성공 (200 OK)")
    void clearCart() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/carts"))
                .andExpect(status().isNoContent());

        verify(cartService).clearCart(memberHolder);
    }

    @Test
    @DisplayName("GET /api/carts : 장바구니 목록 조회 및 정렬 확인 (200 OK)")
    void getCartItems() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();
        // 순서가 섞인 데이터를 리턴한다고 가정 (최신순 정렬 로직 검증 위해)
        CartDto itemOld = CartDto.builder().bookId(1L).cartQuantity(1).createdAt(now.minusHours(2)).build();
        CartDto itemNew = CartDto.builder().bookId(2L).cartQuantity(2).createdAt(now).build();

        given(cartService.getCartItems(memberHolder)).willReturn(List.of(itemOld, itemNew));

        // when & then
        mockMvc.perform(get("/api/carts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                // 정렬 로직 (reversed) 확인: New -> Old 순서여야 함
                .andExpect(jsonPath("$[0].bookId").value(2L)) // itemNew
                .andExpect(jsonPath("$[1].bookId").value(1L)); // itemOld

        verify(cartService).getCartItems(memberHolder);
    }

    @Test
    @DisplayName("GET /api/carts/count : 장바구니 개수 조회 (200 OK)")
    void countCartItems() throws Exception {
        // given
        given(cartService.countCartItems(memberHolder)).willReturn(10L);

        // when & then
        mockMvc.perform(get("/api/carts/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("10"));

        verify(cartService).countCartItems(memberHolder);
    }

    @Test
    @DisplayName("POST /api/carts/merge : 장바구니 병합 (200 OK)")
    void mergeCart() throws Exception {
        // given
        // setUp에서 등록한 ArgumentResolver에 의해:
        // 첫 번째 파라미터(MemberHolder) -> memberHolder 주입
        // 두 번째 파라미터(@GuestOnly) -> guestHolder 주입됨

        // when & then
        mockMvc.perform(post("/api/carts/merge"))
                .andExpect(status().isNoContent());

        // Verify: 올바른 홀더들이 순서대로 넘어갔는지 확인
        verify(cartService).mergeCart(eq(memberHolder), eq(guestHolder));
    }
}