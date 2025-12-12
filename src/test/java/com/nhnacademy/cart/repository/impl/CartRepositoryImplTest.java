package com.nhnacademy.cart.repository.impl;

import com.nhnacademy.cart.domain.EntityCart;
import com.nhnacademy.cart.domain.RedisCart;
import com.nhnacademy.cart.dto.CartDto;
import com.nhnacademy.cart.dto.CartHolder;
import com.nhnacademy.cart.dto.CartSummaryDto;
import com.nhnacademy.cart.repository.CartJpaRepository;
import com.nhnacademy.cart.repository.CartRedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartRepositoryImplTest {

    @InjectMocks
    private CartRepositoryImpl cartRepository;

    @Mock
    private CartJpaRepository jpaRepo;

    @Mock
    private CartRedisRepository redisRepo;

    private CartHolder memberHolder;
    private CartHolder guestHolder;
    private CartDto cartDto;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        memberHolder = CartHolder.member(1L);
        guestHolder = CartHolder.guest("guest-123");
        now = LocalDateTime.now();
        cartDto = CartDto.builder()
                .bookId(100L)
                .cartQuantity(2)
                .createdAt(now)
                .build();
    }

    // ======================================================================
    //  WRITE Operations Tests
    // ======================================================================

    @Test
    @DisplayName("save: 정상 저장 (Redis put 호출 확인)")
    void save_Success() {
        // when
        cartRepository.save(memberHolder, cartDto);

        // then
        verify(redisRepo).put(eq(memberHolder), any(RedisCart.class));
    }

    @Test
    @DisplayName("save: DTO가 null이면 아무것도 하지 않음")
    void save_NullDto() {
        // when
        cartRepository.save(memberHolder, null);

        // then
        verify(redisRepo, never()).put(any(), any());
    }

    @Test
    @DisplayName("saveAll: 정상 일괄 저장 (Redis putAll 호출 확인)")
    void saveAll_Success() {
        // given
        List<CartDto> dtos = List.of(cartDto);

        // when
        cartRepository.saveAll(memberHolder, dtos);

        // then
        verify(redisRepo).putAll(eq(memberHolder), anyList());
    }

    @Test
    @DisplayName("saveAll: 리스트가 비어있으면 무시")
    void saveAll_Empty() {
        // when
        cartRepository.saveAll(memberHolder, Collections.emptyList());

        // then
        verify(redisRepo, never()).putAll(any(), any());
    }

    @Test
    @DisplayName("delete: Redis delete 호출 확인")
    void delete() {
        // when
        cartRepository.delete(memberHolder, 100L);

        // then
        verify(redisRepo).delete(memberHolder, 100L);
    }

    @Test
    @DisplayName("deleteAll: Redis deleteAll 호출 확인")
    void deleteAll() {
        // when
        cartRepository.deleteAll(memberHolder);

        // then
        verify(redisRepo).deleteAll(memberHolder);
    }

    // ======================================================================
    //  READ Operations: tryWarmUpAndGet Logic & findAll
    // ======================================================================

    @Test
    @DisplayName("findAll(WarmUp Skip): 비회원은 워밍업 안 함 -> Redis 조회")
    void findAll_Guest_SkipsWarmUp() {
        // given
        RedisCart redisCart = RedisCart.create(100L, 1, now);
        given(redisRepo.findAll(guestHolder)).willReturn(List.of(redisCart));

        // when
        List<CartDto> result = cartRepository.findAll(guestHolder);

        // then
        assertThat(result).hasSize(1);
        verify(jpaRepo, never()).findAllByMemberId(any()); // DB 조회 없음
        verify(redisRepo).findAll(guestHolder);
    }

    @Test
    @DisplayName("findAll(WarmUp Skip): 이미 Redis 키가 존재함 -> Redis 조회")
    void findAll_HasKey_SkipsWarmUp() {
        // given
        given(redisRepo.hasKey(memberHolder)).willReturn(true);
        RedisCart redisCart = RedisCart.create(100L, 1, now);
        given(redisRepo.findAll(memberHolder)).willReturn(List.of(redisCart));

        // when
        List<CartDto> result = cartRepository.findAll(memberHolder);

        // then
        assertThat(result).hasSize(1);
        verify(jpaRepo, never()).findAllByMemberId(any());
        verify(redisRepo).findAll(memberHolder);
    }

    @Test
    @DisplayName("findAll(WarmUp Skip): 의도적 삭제(Empty Mark) 상태 -> Redis 조회(빈 리스트 예상)")
    void findAll_MarkedEmpty_SkipsWarmUp() {
        // given
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(memberHolder.getMemberId())).willReturn(true);
        given(redisRepo.findAll(memberHolder)).willReturn(Collections.emptyList());

        // when
        List<CartDto> result = cartRepository.findAll(memberHolder);

        // then
        assertThat(result).isEmpty();
        verify(jpaRepo, never()).findAllByMemberId(any());
    }

    @Test
    @DisplayName("findAll(WarmUp Run): DB 데이터 존재 -> Redis Restore 후 즉시 반환 (Double Trip 방지)")
    void findAll_WarmUp_ReturnsDbData() {
        // given
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(memberHolder.getMemberId())).willReturn(false);

        EntityCart entityCart = EntityCart.builder().bookId(100L).cartQuantity(2).createdAt(now).build();
        given(jpaRepo.findAllByMemberId(memberHolder.getMemberId())).willReturn(List.of(entityCart));

        // when
        List<CartDto> result = cartRepository.findAll(memberHolder);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBookId()).isEqualTo(100L);
        verify(redisRepo).restore(eq(memberHolder), anyList());
        verify(redisRepo, never()).findAll(memberHolder); // Redis 재조회 없어야 함
    }

    @Test
    @DisplayName("findAll(WarmUp Run): DB도 비어있음 -> Redis Restore(Empty) 후 빈 리스트 반환")
    void findAll_WarmUp_DbEmpty() {
        // given
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(1L)).willReturn(false);
        given(jpaRepo.findAllByMemberId(1L)).willReturn(Collections.emptyList());

        // when
        List<CartDto> result = cartRepository.findAll(memberHolder);

        // then
        assertThat(result).isEmpty();
        verify(redisRepo).restore(eq(memberHolder), eq(Collections.emptyList()));
        verify(redisRepo, never()).findAll(memberHolder);
    }

    // ======================================================================
    //  Other READ Operations Tests
    // ======================================================================

    @Test
    @DisplayName("findByBookId: 워밍업 된 데이터(메모리)에서 찾기")
    void findByBookId_Optimized() {
        // given: DB에 데이터 2개 존재 가정
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(1L)).willReturn(false);

        EntityCart target = EntityCart.builder().bookId(100L).cartQuantity(1).createdAt(now).build();
        EntityCart other = EntityCart.builder().bookId(200L).cartQuantity(1).createdAt(now).build();
        given(jpaRepo.findAllByMemberId(1L)).willReturn(List.of(target, other));

        // when
        Optional<CartDto> result = cartRepository.findByBookId(memberHolder, 100L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getBookId()).isEqualTo(100L);
        verify(redisRepo, never()).findByBookId(any(), any());
    }

    @Test
    @DisplayName("findByBookId: 기존 캐시(Redis) 이용")
    void findByBookId_Cached() {
        // given
        given(redisRepo.hasKey(memberHolder)).willReturn(true);
        RedisCart redisCart = RedisCart.create(100L, 1, now);
        given(redisRepo.findByBookId(memberHolder, 100L)).willReturn(Optional.of(redisCart));

        // when
        Optional<CartDto> result = cartRepository.findByBookId(memberHolder, 100L);

        // then
        assertThat(result).isPresent();
        verify(redisRepo).findByBookId(memberHolder, 100L);
    }

    @Test
    @DisplayName("existsByBookId: 워밍업 된 데이터(메모리)에서 확인")
    void existsByBookId_Optimized() {
        // given
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(1L)).willReturn(false);
        EntityCart entity = EntityCart.builder().bookId(100L).cartQuantity(1).createdAt(now).build();
        given(jpaRepo.findAllByMemberId(1L)).willReturn(List.of(entity));

        // when
        boolean exists = cartRepository.existsByBookId(memberHolder, 100L);

        // then
        assertThat(exists).isTrue();
        verify(redisRepo, never()).existsByBookId(any(), any());
    }

    @Test
    @DisplayName("existsByBookId: 캐시도 없고 DB 워밍업도 안 된 상태 (return false)")
    void existsByBookId_Nothing() {
        // given: Empty Marked
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(1L)).willReturn(true);

        // when
        boolean exists = cartRepository.existsByBookId(memberHolder, 100L);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("count: 워밍업 된 데이터 사이즈 반환")
    void count_Optimized() {
        // given
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(1L)).willReturn(false);
        EntityCart entity = EntityCart.builder().bookId(100L).cartQuantity(1).createdAt(now).build();
        given(jpaRepo.findAllByMemberId(1L)).willReturn(List.of(entity));

        // when
        long count = cartRepository.countDistinctCartItem(memberHolder);

        // then
        assertThat(count).isEqualTo(1);
        verify(redisRepo, never()).count(any());
    }

    @Test
    @DisplayName("count: 기존 캐시 이용")
    void count_Cached() {
        // given
        given(redisRepo.hasKey(memberHolder)).willReturn(true);
        given(redisRepo.count(memberHolder)).willReturn(5L);

        // when
        long count = cartRepository.countDistinctCartItem(memberHolder);

        // then
        assertThat(count).isEqualTo(5);
    }

    @Test
    @DisplayName("count: 캐시 없음 & 워밍업 불가 -> 0")
    void count_Zero() {
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(1L)).willReturn(true);

        long count = cartRepository.countDistinctCartItem(memberHolder);

        assertThat(count).isEqualTo(0);
    }

    // ======================================================================
    //  getSummary Tests (NEW)
    // ======================================================================

    @Test
    @DisplayName("getSummary: 워밍업 된 데이터로 요약정보 계산 (Optimized)")
    void getSummary_Optimized() {
        // given: DB에 2권짜리, 3권짜리 책이 있음
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(1L)).willReturn(false);

        EntityCart item1 = EntityCart.builder().bookId(1L).cartQuantity(2).createdAt(now).build();
        EntityCart item2 = EntityCart.builder().bookId(2L).cartQuantity(3).createdAt(now).build();
        given(jpaRepo.findAllByMemberId(1L)).willReturn(List.of(item1, item2));

        // when
        CartSummaryDto summary = cartRepository.getSummary(memberHolder);

        // then
        // Line Count: 2개 (item1, item2)
        // Total Quantity: 5개 (2 + 3)
        assertThat(summary).isNotNull();
        assertThat(summary.getLineCount()).isEqualTo(2);
        assertThat(summary.getTotalQuantity()).isEqualTo(5);
        verify(redisRepo, never()).findAll(any()); // Redis 조회 안 함
    }

    @Test
    @DisplayName("getSummary: 기존 캐시 데이터로 요약정보 계산 (Cached)")
    void getSummary_Cached() {
        // given
        given(redisRepo.hasKey(memberHolder)).willReturn(true); // 워밍업 스킵

        RedisCart redisItem1 = RedisCart.create(1L, 2, now);
        RedisCart redisItem2 = RedisCart.create(2L, 5, now);
        given(redisRepo.findAll(memberHolder)).willReturn(List.of(redisItem1, redisItem2));

        // when
        CartSummaryDto summary = cartRepository.getSummary(memberHolder);

        // then
        assertThat(summary.getLineCount()).isEqualTo(2);
        assertThat(summary.getTotalQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("getSummary: 캐시도 없고 데이터도 없음 (Zero)")
    void getSummary_Nothing() {
        // given: Empty Marked -> tryWarmUp returns null
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(1L)).willReturn(true);
        // redisRepo.hasKey()가 위에서 false였으므로, 아래 if문도 통과 못함

        // when
        CartSummaryDto summary = cartRepository.getSummary(memberHolder);

        // then
        assertThat(summary.getLineCount()).isEqualTo(0);
        assertThat(summary.getTotalQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("getSummary: (Edge Case) 워밍업 했으나 DB가 비어있는 경우")
    void getSummary_Optimized_EmptyDB() {
        // given
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(1L)).willReturn(false);
        given(jpaRepo.findAllByMemberId(1L)).willReturn(Collections.emptyList());

        // when
        CartSummaryDto summary = cartRepository.getSummary(memberHolder);

        // then: createSummaryFromList 내부 로직 검증
        assertThat(summary.getLineCount()).isEqualTo(0);
        assertThat(summary.getTotalQuantity()).isEqualTo(0);
    }

    // ======================================================================
    //  warmUp Public Method Test
    // ======================================================================

    @Test
    @DisplayName("warmUp: public 메소드 호출 확인")
    void warmUp_PublicMethod() {
        // given
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(1L)).willReturn(false);
        EntityCart entity = EntityCart.builder().bookId(100L).cartQuantity(1).createdAt(now).build();
        given(jpaRepo.findAllByMemberId(1L)).willReturn(List.of(entity));

        // when
        cartRepository.warmUp(memberHolder);

        // then
        verify(jpaRepo).findAllByMemberId(1L);
        verify(redisRepo).restore(eq(memberHolder), anyList());
    }
}