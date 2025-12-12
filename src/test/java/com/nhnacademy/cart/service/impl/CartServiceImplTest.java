package com.nhnacademy.cart.service.impl;

import com.nhnacademy.cart.common.config.CartProperties;
import com.nhnacademy.cart.common.exception.CartCapacityExceededException;
import com.nhnacademy.cart.common.exception.CartNotFoundException;
import com.nhnacademy.cart.common.exception.InvalidCartQuantityException;
import com.nhnacademy.cart.dto.CartDto;
import com.nhnacademy.cart.dto.CartHolder;
import com.nhnacademy.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @InjectMocks
    private CartServiceImpl cartService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartProperties cartProperties;

    private CartHolder memberHolder;
    private CartHolder guestHolder;
    private CartDto cartDto;

    @BeforeEach
    void setUp() {
        memberHolder = CartHolder.member(1L);
        guestHolder = CartHolder.guest("guest-123");
        cartDto = CartDto.builder()
                .bookId(100L)
                .cartQuantity(1)
                .build();

        // 기본 설정 (필요한 테스트에서 오버라이딩 가능)
        lenient().when(cartProperties.getMaxItems()).thenReturn(100);
    }

    // ======================================================================
    //  addCartItem Tests
    // ======================================================================

    @Test
    @DisplayName("addCartItem: 수량이 0 이하일 때 예외 발생")
    void addCartItem_InvalidQuantity() {
        CartDto invalidDto = CartDto.builder().bookId(1L).cartQuantity(0).build();

        assertThatThrownBy(() -> cartService.addCartItem(memberHolder, invalidDto))
                .isInstanceOf(InvalidCartQuantityException.class)
                .hasMessage("수량은 1개 이상이어야 합니다.");
    }

    @Test
    @DisplayName("addCartItem: 이미 있는 상품일 경우 수량 합산")
    void addCartItem_ExistingItem() {
        // given
        CartDto existingItem = CartDto.builder()
                .bookId(100L).cartQuantity(2).createdAt(LocalDateTime.now().minusHours(1))
                .build();
        given(cartRepository.findByBookId(memberHolder, 100L)).willReturn(Optional.of(existingItem));

        // when
        cartService.addCartItem(memberHolder, cartDto); // quantity 1

        // then
        ArgumentCaptor<CartDto> captor = ArgumentCaptor.forClass(CartDto.class);
        verify(cartRepository).save(eq(memberHolder), captor.capture());

        CartDto savedDto = captor.getValue();
        assertThat(savedDto.getBookId()).isEqualTo(100L);
        assertThat(savedDto.getCartQuantity()).isEqualTo(3); // 2 + 1
        // 로직상 추가 담기 시에는 createdAt을 현재 시간(addCartItem 호출 시점)으로 갱신하도록 되어있음
        assertThat(savedDto.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("addCartItem: 없는 상품이고 용량이 충분할 때 저장")
    void addCartItem_NewItem_CapacityOk() {
        // given
        given(cartRepository.findByBookId(memberHolder, 100L)).willReturn(Optional.empty());
        given(cartRepository.countDistinctCartItem(memberHolder)).willReturn(10L); // 현재 10개, 최대 100개

        // when
        cartService.addCartItem(memberHolder, cartDto);

        // then
        verify(cartRepository).save(eq(memberHolder), any(CartDto.class));
    }

    @Test
    @DisplayName("addCartItem: 용량 초과 시 예외 발생")
    void addCartItem_CapacityExceeded() {
        // given
        given(cartRepository.findByBookId(memberHolder, 100L)).willReturn(Optional.empty());
        given(cartRepository.countDistinctCartItem(memberHolder)).willReturn(100L); // 이미 꽉 참
        given(cartProperties.getMaxItems()).willReturn(100);

        // when & then
        assertThatThrownBy(() -> cartService.addCartItem(memberHolder, cartDto))
                .isInstanceOf(CartCapacityExceededException.class);
    }

    // ======================================================================
    //  updateCartItem Tests
    // ======================================================================

    @Test
    @DisplayName("updateCartItem: 수량이 0 이하일 때 예외 발생")
    void updateCartItem_InvalidQuantity() {
        assertThatThrownBy(() -> cartService.updateCartItem(memberHolder, 100L, 0))
                .isInstanceOf(InvalidCartQuantityException.class);
    }

    @Test
    @DisplayName("updateCartItem: 존재하지 않는 상품 수정 시 예외 발생")
    void updateCartItem_NotFound() {
        given(cartRepository.findByBookId(memberHolder, 100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateCartItem(memberHolder, 100L, 5))
                .isInstanceOf(CartNotFoundException.class);
    }

    @Test
    @DisplayName("updateCartItem: 정상 수정 (기존 createdAt 유지 확인)")
    void updateCartItem_Success() {
        // given
        LocalDateTime oldTime = LocalDateTime.now().minusDays(1);
        CartDto existingItem = CartDto.builder()
                .bookId(100L).cartQuantity(1).createdAt(oldTime)
                .build();
        given(cartRepository.findByBookId(memberHolder, 100L)).willReturn(Optional.of(existingItem));

        // when
        cartService.updateCartItem(memberHolder, 100L, 5);

        // then
        ArgumentCaptor<CartDto> captor = ArgumentCaptor.forClass(CartDto.class);
        verify(cartRepository).save(eq(memberHolder), captor.capture());

        CartDto savedDto = captor.getValue();
        assertThat(savedDto.getCartQuantity()).isEqualTo(5);
        assertThat(savedDto.getCreatedAt()).isEqualTo(oldTime); // 시간 유지 필수
    }

    // ======================================================================
    //  removeCartItem Tests
    // ======================================================================

    @Test
    @DisplayName("removeCartItem: 존재하지 않는 상품 삭제 시 예외 발생")
    void removeCartItem_NotFound() {
        given(cartRepository.existsByBookId(memberHolder, 100L)).willReturn(false);

        assertThatThrownBy(() -> cartService.removeCartItem(memberHolder, 100L))
                .isInstanceOf(CartNotFoundException.class);
    }

    @Test
    @DisplayName("removeCartItem: 정상 삭제")
    void removeCartItem_Success() {
        given(cartRepository.existsByBookId(memberHolder, 100L)).willReturn(true);

        cartService.removeCartItem(memberHolder, 100L);

        verify(cartRepository).delete(memberHolder, 100L);
    }

    // ======================================================================
    //  Simple Read/Clear Tests
    // ======================================================================

    @Test
    @DisplayName("clearCart: 전체 삭제 호출 확인")
    void clearCart() {
        cartService.clearCart(memberHolder);
        verify(cartRepository).deleteAll(memberHolder);
    }

    @Test
    @DisplayName("getCartItems: 조회 호출 확인")
    void getCartItems() {
        cartService.getCartItems(memberHolder);
        verify(cartRepository).findAll(memberHolder);
    }

    @Test
    @DisplayName("getCartSummary: 요약정보 조회 호출 확인")
    void getCartSummary() {
        cartService.getCartSummary(memberHolder);
        verify(cartRepository).getSummary(memberHolder);
    }

    // ======================================================================
    //  mergeCart Tests (Complex Logic)
    // ======================================================================

    @Test
    @DisplayName("mergeCart: 잘못된 파라미터(Member <-> Guest 바뀜) 예외")
    void mergeCart_InvalidArgs() {
        // case 1: guestHolder가 target이 됨 (X)
        assertThatThrownBy(() -> cartService.mergeCart(guestHolder, memberHolder))
                .isInstanceOf(IllegalArgumentException.class);

        // case 2: memberHolder가 source가 됨 (X)
        assertThatThrownBy(() -> cartService.mergeCart(memberHolder, CartHolder.member(2L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("mergeCart: 비회원 장바구니가 비어있으면 바로 종료")
    void mergeCart_EmptyGuest() {
        given(cartRepository.findAll(guestHolder)).willReturn(Collections.emptyList());

        cartService.mergeCart(memberHolder, guestHolder);

        verify(cartRepository, never()).findAll(memberHolder);
        verify(cartRepository, never()).saveAll(any(), any());
    }

    @Test
    @DisplayName("mergeCart: 정상 병합 (중복 상품 합산 + 신규 상품 추가 + 정렬)")
    void mergeCart_Success() {
        // given
        LocalDateTime t1 = LocalDateTime.now().minusHours(2);
        LocalDateTime t2 = LocalDateTime.now().minusHours(1);

        // Member: [Book 1 (Qty 2, t1)]
        CartDto memberItem1 = CartDto.builder().bookId(1L).cartQuantity(2).createdAt(t1).build();
        given(cartRepository.findAll(memberHolder)).willReturn(List.of(memberItem1));

        // Guest: [Book 1 (Qty 3, t2)], [Book 2 (Qty 1, t2)]
        CartDto guestItem1 = CartDto.builder().bookId(1L).cartQuantity(3).createdAt(t2).build();
        CartDto guestItem2 = CartDto.builder().bookId(2L).cartQuantity(1).createdAt(t2).build();
        given(cartRepository.findAll(guestHolder)).willReturn(List.of(guestItem1, guestItem2));

        // when
        cartService.mergeCart(memberHolder, guestHolder);

        // then
        // 1. 기존꺼 지우고 새로 저장하는지 확인
        verify(cartRepository).deleteAll(memberHolder);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CartDto>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(cartRepository).saveAll(eq(memberHolder), listCaptor.capture());

        List<CartDto> savedList = listCaptor.getValue();
        assertThat(savedList).hasSize(2);

        // Book 1: 합산 확인 (2+3=5), 시간은 Member 기준(t1) 유지 확인
        CartDto mergedBook1 = savedList.stream().filter(c -> c.getBookId().equals(1L)).findFirst().get();
        assertThat(mergedBook1.getCartQuantity()).isEqualTo(5);
        assertThat(mergedBook1.getCreatedAt()).isEqualTo(t1);

        // Book 2: 신규 추가 확인
        CartDto mergedBook2 = savedList.stream().filter(c -> c.getBookId().equals(2L)).findFirst().get();
        assertThat(mergedBook2.getCartQuantity()).isEqualTo(1);

        // 정렬 확인 (t2가 t1보다 최신이므로 Book2가 먼저 와야 함)
        // t1: 2시간 전, t2: 1시간 전 -> t2 > t1
        // 정렬 로직: t2.compareTo(t1) (내림차순) -> Book 2, Book 1 순서
        assertThat(savedList.get(0).getBookId()).isEqualTo(2L);
        assertThat(savedList.get(1).getBookId()).isEqualTo(1L);

        // 게스트 카트 삭제 확인
        verify(cartRepository).deleteAll(guestHolder);
    }

    @Test
    @DisplayName("mergeCart: 최대 개수 초과 시 오래된 상품 삭제 (Truncate)")
    void mergeCart_Truncate() {
        // given
        given(cartProperties.getMaxItems()).willReturn(2); // 최대 2개만 허용

        LocalDateTime now = LocalDateTime.now();
        // Member: [Book 1 (Oldest)]
        CartDto item1 = CartDto.builder().bookId(1L).cartQuantity(1).createdAt(now.minusHours(10)).build();
        given(cartRepository.findAll(memberHolder)).willReturn(List.of(item1));

        // Guest: [Book 2 (Mid)], [Book 3 (Newest)]
        CartDto item2 = CartDto.builder().bookId(2L).cartQuantity(1).createdAt(now.minusHours(5)).build();
        CartDto item3 = CartDto.builder().bookId(3L).cartQuantity(1).createdAt(now).build();
        given(cartRepository.findAll(guestHolder)).willReturn(List.of(item2, item3));

        // when
        cartService.mergeCart(memberHolder, guestHolder);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CartDto>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(cartRepository).saveAll(eq(memberHolder), listCaptor.capture());

        List<CartDto> savedList = listCaptor.getValue();

        // 3개 중 상위 2개만 남아야 함
        assertThat(savedList).hasSize(2);

        // 최신순 정렬 시: Item3(Newest) -> Item2(Mid) -> Item1(Oldest)
        // 따라서 Item1이 삭제되어야 함
        assertThat(savedList).extracting("bookId").containsExactly(3L, 2L);
    }
}