package com.nhnacademy.cart.service.impl;

import com.nhnacademy.cart.common.config.CartProperties;
import com.nhnacademy.cart.common.exception.CartCapacityExceededException;
import com.nhnacademy.cart.common.exception.CartNotFoundException;
import com.nhnacademy.cart.common.exception.InvalidCartQuantityException;
import com.nhnacademy.cart.dto.CartDto;
import com.nhnacademy.cart.dto.CartHolder;
import com.nhnacademy.cart.repository.CartRepository;
import com.nhnacademy.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartProperties cartProperties;

    @Override
    @Transactional
    public void addCartItem(CartHolder holder, CartDto requestDto) {
        if (requestDto.getCartQuantity() <= 0) {
            throw new InvalidCartQuantityException("수량은 1개 이상이어야 합니다.");
        }

        // 기존 장바구니 조회 (Redis 우선 -> DB WarmUp)
        Optional<CartDto> existingOpt = cartRepository.findByBookId(holder, requestDto.getBookId());

        int finalQuantity = requestDto.getCartQuantity();

        if (existingOpt.isPresent()) {
            // [이미 있는 상품]
            CartDto existing = existingOpt.get();
            finalQuantity += existing.getCartQuantity();
        } else {
            // [없는 상품] 개수 제한 확인
            long currentCount = cartRepository.count(holder);
            if (currentCount >= cartProperties.getMaxItems()) {
                throw new CartCapacityExceededException(
                        "장바구니에는 최대 " + cartProperties.getMaxItems() + "종류의 상품만 담을 수 있습니다."
                );
            }
        }

        // 담기/추가담기 시에는 담은 시간을 항상 지금으로 설정
        CartDto toSave = CartDto.builder()
                .bookId(requestDto.getBookId())
                .cartQuantity(finalQuantity)
                .createdAt(LocalDateTime.now())
                .build();

        cartRepository.save(holder, toSave);
    }

    @Override
    @Transactional
    public void updateCartItem(CartHolder holder, Long bookId, Integer quantity) {
        if (quantity <= 0) {
            throw new InvalidCartQuantityException("수량은 1개 이상이어야 합니다.");
        }

        // 기존 장바구니 조회 (Redis 우선 -> DB WarmUp)
        CartDto existing = cartRepository.findByBookId(holder, bookId)
                .orElseThrow(() -> new CartNotFoundException("장바구니에 해당 상품이 존재하지 않습니다."));

        CartDto updateDto = CartDto.builder()
                .bookId(bookId)
                .cartQuantity(quantity)
                .createdAt(existing.getCreatedAt())
                .build();

        cartRepository.save(holder, updateDto);
    }

    @Override
    @Transactional
    public void removeCartItem(CartHolder holder, Long bookId) {
        if (!cartRepository.existsByBookId(holder, bookId)) {
            throw new CartNotFoundException("장바구니에 해당 상품이 이미 존재하지 않습니다.");
        }

        cartRepository.delete(holder, bookId);
    }

    @Override
    @Transactional
    public void clearCart(CartHolder holder) {
        cartRepository.deleteAll(holder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartDto> getCartItems(CartHolder holder) {
        return cartRepository.findAll(holder);
    }

    @Override
    @Transactional(readOnly = true)
    public long countCartItems(CartHolder holder) {
        return cartRepository.count(holder);
    }

    @Override
    @Transactional
    public void mergeCart(CartHolder memberHolder, CartHolder guestHolder) {
        // 방어 로직
        if (!memberHolder.isMember() || guestHolder.isMember()) {
            throw new IllegalArgumentException("Merge 요청 파라미터가 뒤바뀌었습니다. (Member <-> Guest)");
        }

        // 비회원 장바구니 조회
        List<CartDto> guestItems = cartRepository.findAll(guestHolder);
        if (guestItems.isEmpty()) return;

        // 회원 장바구니 조회
        List<CartDto> memberItems = cartRepository.findAll(memberHolder);

        // 메모리 병합
        Map<Long, CartDto> mergeMap = new HashMap<>();

        // 회원의 기존 장바구니
        for (CartDto memberItem : memberItems) {
            mergeMap.put(memberItem.getBookId(), memberItem);
        }
        // 비회원 장바구니 합치기
        for (CartDto guestItem : guestItems) {
            if (mergeMap.containsKey(guestItem.getBookId())) {
                CartDto exist = mergeMap.get(guestItem.getBookId());

                CartDto merged = CartDto.builder()
                        .bookId(exist.getBookId())
                        .cartQuantity(exist.getCartQuantity() + guestItem.getCartQuantity())
                        .createdAt(exist.getCreatedAt()) // 담은 시간은 회원 기준으로
                        .build();

                mergeMap.put(exist.getBookId(), merged);

            } else {
                // 신규 상품: 그대로 추가
                mergeMap.put(guestItem.getBookId(), guestItem);
            }
        }

        // 리스트 변환 및 정렬
        List<CartDto> mergedList = new ArrayList<>(mergeMap.values());

        // 최신순 정렬
        mergedList.sort((o1, o2) -> {
            LocalDateTime t1 = o1.getCreatedAt() == null ? LocalDateTime.now() : o1.getCreatedAt();
            LocalDateTime t2 = o2.getCreatedAt() == null ? LocalDateTime.now() : o2.getCreatedAt();
            return t2.compareTo(t1);
        });

        // 최신 기준으로 개수 제한... 넘칠 경우 오래된 상품 부터 삭제
        if (mergedList.size() > cartProperties.getMaxItems()) {
            mergedList = mergedList.subList(0, cartProperties.getMaxItems());
        }

        // 저장 (Reset Strategy)
        // 기존 회원 장바구니를 비우고(deleteAll) 병합된 리스트를 저장(saveAll)해야
        // 잘려나간 상품이 Redis에 남지 않음
        cartRepository.deleteAll(memberHolder);
        cartRepository.saveAll(memberHolder, mergedList);

        // 비회원 장바구니 정리
        cartRepository.deleteAll(guestHolder);
    }
}