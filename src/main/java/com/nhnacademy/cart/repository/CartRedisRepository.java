package com.nhnacademy.cart.repository;

import com.nhnacademy.cart.domain.RedisCart;
import com.nhnacademy.cart.dto.CartHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CartRedisRepository {
    // [단건 저장/수정]
    void put(CartHolder holder, RedisCart cart);

    // [다건 저장]
    void putAll(CartHolder holder, List<RedisCart> carts);

    // [Cache Warming] DB 데이터를 Redis로 복구
    void restore(CartHolder holder, List<RedisCart> carts);

    // [단건 조회]
    Optional<RedisCart> findByBookId(CartHolder holder, Long bookId);

    // [목록 조회]
    List<RedisCart> findAll(CartHolder holder);

    // [장바구니 아이템 체크]
    boolean existsByBookId(CartHolder holder, Long bookId);

    // [단건 삭제]
    void delete(CartHolder holder, Long bookId);

    // [전체 삭제]
    void deleteAll(CartHolder holder);

    // [개수 조회]
    long count(CartHolder holder);

    // [키 존재 여부]
    boolean hasKey(CartHolder holder);

    // ======================================================================
    // [스케줄러용 SPOP] - 변경된 회원 ID 목록을 꺼내고(Pop) 삭제
    // Write-Back 동시성 문제를 해결하기 위해, SPOP 을 사용...
    // ======================================================================
    List<Object> popDirtyMemberIds(long count);

    //해당 회원이 '의도적으로 전체 삭제'를 했는지 확인
    boolean isMarkedAsEmpty(Long memberId);

    //Empty 마킹을 정리
    void removeEmptyMark(Long memberId);

    /**
     * 회원 ID 목록 중 Dirty Set에 포함되지 않은(Clean한) 회원 ID만 걸러냄
     * (Dirty한 회원은 현재 활동 중이거나 동기화 대기 중이므로 삭제하면 안 됨)
     */
    List<Long> filterCleanMemberIds(Set<Long> memberIds);

    /**
     * [Scheduler 용도] 여러 회원의 장바구니 데이터를 일괄 삭제. (Data + Dirty Set + Empty ZSet)
     */
    void deleteMembersBatch(Set<Long> memberIds);

    /**
     * [Scheduler 용도] 동기화 실패 시 다시 Dirty Set에 추가하기 위한 메소드
     */
    void addDirtyMember(Long memberId);

    /**
     * [Scheduler 용도] 오래된 Empty Mark(ZSet)를 정리하여 Redis 메모리를 확보
     * @param maxScore 삭제할 기준 시간 (이 시간보다 이전의 마킹은 삭제)
     * @return 삭제된 개수
     */
    long removeOldEmptyMarks(double maxScore);
}