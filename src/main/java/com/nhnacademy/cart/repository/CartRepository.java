package com.nhnacademy.cart.repository;

import com.nhnacademy.cart.dto.CartDto;
import com.nhnacademy.cart.dto.CartHolder;
import com.nhnacademy.cart.dto.CartSummaryDto;

import java.util.List;
import java.util.Optional;

/**
 * 회원/비회원 장바구니 데이터 처리를 위한 통합 리포지토리.
 * 데이터 정합성과 성능의 균형을 위해 아래 전략을 선택
 *
 * Write (CUD): Cache Invalidation
 * - DB에 먼저 반영하고, Redis 캐시는 무조건 삭제
 *
 * Read (R): Look Aside
 * - 캐시 조회 실패 시 DB에서 로딩하여 캐시를 갱신(Warming).
 */
public interface CartRepository {

    /**
     * [단건 저장]
     * - Strategy: Cache Invalidation
     */
    void save(CartHolder holder, CartDto cartDto);

    /**
     * [장바구니 전체 저장]
     * DTO 리스트를 받아 DB에 일괄 반영(Batch)하고, Redis 캐시를 무효화...
     */
    void saveAll(CartHolder holder, List<CartDto> cartDtos);

    /**
     * [단건 삭제]
     * - Strategy: Cache Invalidation (DB 삭제 후 Redis 캐시 삭제)
     */
    void delete(CartHolder holder, Long bookId);

    /**
     * [전체 삭제]
     * - Strategy: Cache Invalidation (DB 전체 삭제 후 Redis 전체 삭제)
     */
    void deleteAll(CartHolder holder);

    /**
     * [워밍]
     * 장바구니 첫 진입, TTL 만료등의 사유로 캐시가 비었을 때 사용
     */
    void warmUp(CartHolder holder);

    /**
     * [단건 조회]
     * - Strategy: Look Aside
     */
    Optional<CartDto> findByBookId(CartHolder holder, Long bookId);

    /**
     * [장바구니 아이템 체크]
     */
    boolean existsByBookId(CartHolder holder, Long bookId);

    /**
     * [목록 조회]
     * - Strategy: Look Aside
     * - Redis 캐시를 우선 조회하며, Miss 발생 시 DB에서 데이터를 로딩하여 캐시를 갱신
     */
    List<CartDto> findAll(CartHolder holder);

    /**
     * [담긴 상품 종류 조회]
     * - Strategy: Redis Priority
     *  Redis 캐시를 최우선으로 신뢰
     * - ... 이미, 다른 로직에서 상당한 정합성을 보장
     */
    long countDistinctCartItem(CartHolder holder);

    /**
     * [담긴 상품 종류 + 담긴 상품 총 개수, 요약정보 조회]
     * Redis 캐시를 최우선으로 신뢰
     */
    CartSummaryDto getSummary(CartHolder holder);
}