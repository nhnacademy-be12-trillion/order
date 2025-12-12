package com.nhnacademy.cart.repository;

import com.nhnacademy.cart.domain.EntityCart;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CartJpaRepository extends JpaRepository<EntityCart, Long> {

    // [단건 조회]
    Optional<EntityCart> findByMemberIdAndBookId(Long memberId, Long bookId);

    // [목록 조회]
    List<EntityCart> findAllByMemberId(Long memberId);


    // [단건 삭제]
    void deleteByMemberIdAndBookId(Long memberId, Long bookId);

    // [개수 조회]
    long countByMemberId(Long memberId);

    // [장바구니 아이템 체크]
    boolean existsByMemberIdAndBookId(Long memberId, Long bookId);

    /**
     * [스케줄러용 조회]
     * 기준일(threshold)보다 오래된 아이템의 PK(cartId)와 소유자(memberId)를 조회합니다.
     * 인덱스가 걸려 있어야 성능이 나옴...
     */
    @Query("SELECT c.cartId, c.memberId FROM EntityCart c WHERE c.createdAt < :threshold")
    List<Object[]> findOldItemIdsAndMemberIds(@Param("threshold") LocalDateTime threshold, Pageable pageable);

    /**
     * [스케줄러용 삭제]
     * PK 리스트를 받아 한 번에 삭제합니다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM EntityCart c WHERE c.cartId IN :cartIds")
    int deleteByCartIds(@Param("cartIds") List<Long> cartIds);

    // [성능 최적화] Select 없이 바로 Delete 날림
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM EntityCart c WHERE c.memberId = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}