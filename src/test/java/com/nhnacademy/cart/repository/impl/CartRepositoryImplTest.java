package com.nhnacademy.cart.repository.impl;

import com.nhnacademy.cart.domain.EntityCart;
import com.nhnacademy.cart.domain.RedisCart;
import com.nhnacademy.cart.dto.CartDto;
import com.nhnacademy.cart.dto.CartHolder;
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
    //  tryWarmUpAndGet Branch Tests (핵심 로직)
    //  findAll 메소드를 통해 내부 로직을 검증합니다.
    // ======================================================================

    @Test
    @DisplayName("WarmUp Skip: 비회원은 워밍업 안 함 -> Redis 조회")
    void findAll_Guest_SkipsWarmUp() {
        // given
        RedisCart redisCart = RedisCart.create(100L, 1, now);
        given(redisRepo.findAll(guestHolder)).willReturn(List.of(redisCart));

        // when
        List<CartDto> result = cartRepository.findAll(guestHolder);

        // then
        assertThat(result).hasSize(1);
        verify(jpaRepo, never()).findAllByMemberId(any()); // DB 조회 없어야 함
        verify(redisRepo).findAll(guestHolder);
    }

    @Test
    @DisplayName("WarmUp Skip: 이미 Redis 키가 존재함 -> Redis 조회")
    void findAll_HasKey_SkipsWarmUp() {
        // given
        given(redisRepo.hasKey(memberHolder)).willReturn(true);
        RedisCart redisCart = RedisCart.create(100L, 1, now);
        given(redisRepo.findAll(memberHolder)).willReturn(List.of(redisCart));

        // when
        List<CartDto> result = cartRepository.findAll(memberHolder);

        // then
        assertThat(result).hasSize(1);
        verify(jpaRepo, never()).findAllByMemberId(any()); // DB 조회 없어야 함
        verify(redisRepo).findAll(memberHolder);
    }

    @Test
    @DisplayName("WarmUp Skip: 의도적 삭제(Empty Mark) 상태 -> Redis 조회(빈 리스트)")
    void findAll_MarkedEmpty_SkipsWarmUp() {
        // given
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(memberHolder.getMemberId())).willReturn(true);
        given(redisRepo.findAll(memberHolder)).willReturn(Collections.emptyList());

        // when
        List<CartDto> result = cartRepository.findAll(memberHolder);

        // then
        assertThat(result).isEmpty();
        verify(jpaRepo, never()).findAllByMemberId(any()); // DB 조회 없어야 함
    }

    @Test
    @DisplayName("WarmUp Run: DB 조회 성공 -> Redis Restore 후 데이터 반환 (Redis 재조회 X)")
    void findAll_WarmUp_ReturnsDbData() {
        // given: 워밍업 조건 충족 (Member, No Key, Not Empty)
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(memberHolder.getMemberId())).willReturn(false);

        // DB에 데이터가 있음
        EntityCart entityCart = EntityCart.builder().bookId(100L).cartQuantity(2).createdAt(now).build();
        given(jpaRepo.findAllByMemberId(memberHolder.getMemberId())).willReturn(List.of(entityCart));

        // when
        List<CartDto> result = cartRepository.findAll(memberHolder);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBookId()).isEqualTo(100L);

        // [핵심] restore가 호출되어야 하고, redisRepo.findAll()은 호출되지 않아야 함 (Double Network Trip 방지)
        verify(redisRepo).restore(eq(memberHolder), anyList());
        verify(redisRepo, never()).findAll(memberHolder);
    }

    @Test
    @DisplayName("WarmUp Run: DB도 비어있음 -> Redis Restore(Empty) 후 빈 리스트 반환")
    void findAll_WarmUp_DbEmpty() {
        // given
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(1L)).willReturn(false);
        given(jpaRepo.findAllByMemberId(1L)).willReturn(Collections.emptyList());

        // when
        List<CartDto> result = cartRepository.findAll(memberHolder);

        // then
        assertThat(result).isEmpty();
        // 빈 리스트라도 restore 호출 (Empty Marking을 위해)
        verify(redisRepo).restore(eq(memberHolder), eq(Collections.emptyList()));
        verify(redisRepo, never()).findAll(memberHolder);
    }

    // ======================================================================
    //  Other READ Operations Tests (findAll 외 메소드들의 최적화 확인)
    // ======================================================================

    @Test
    @DisplayName("findByBookId: 워밍업 된 데이터(메모리)에서 찾기")
    void findByBookId_Optimized() {
        // given: 워밍업 발생 상황
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(1L)).willReturn(false);

        EntityCart entity = EntityCart.builder().bookId(100L).cartQuantity(1).createdAt(now).build();
        given(jpaRepo.findAllByMemberId(1L)).willReturn(List.of(entity));

        // when
        Optional<CartDto> result = cartRepository.findByBookId(memberHolder, 100L);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getBookId()).isEqualTo(100L);
        // Redis 조회(findByBookId)는 호출되지 않아야 함
        verify(redisRepo, never()).findByBookId(any(), any());
    }

    @Test
    @DisplayName("findByBookId: 기존 캐시(Redis) 이용")
    void findByBookId_Cached() {
        // given: 워밍업 스킵 (이미 키 있음)
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
        // given: 워밍업 발생 (DB에 데이터 있음)
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
    @DisplayName("existsByBookId: 기존 캐시 이용")
    void existsByBookId_Cached() {
        // given: 워밍업 스킵
        given(redisRepo.hasKey(memberHolder)).willReturn(true);
        given(redisRepo.existsByBookId(memberHolder, 100L)).willReturn(true);

        // when
        boolean exists = cartRepository.existsByBookId(memberHolder, 100L);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByBookId: 캐시도 없고 DB 워밍업도 안 된 상태 (return false)")
    void existsByBookId_Nothing() {
        // given: 워밍업 스킵 상황 (예: Empty Mark)인데 Redis hasKey도 false인 경우
        // (실제 코드 흐름상 tryWarmUp이 null 리턴하고, redis.hasKey도 false면 false 반환됨)
        given(redisRepo.hasKey(memberHolder)).willReturn(false); // 1. tryWarmUp 안에서 체크
        given(redisRepo.isMarkedAsEmpty(1L)).willReturn(true);   // 2. Empty라서 null 리턴
        // tryWarmUpAndGet returns null.

        // 3. hasKey 재호출 (if statement)
        // Mockito는 같은 메소드 호출에 대해 순서대로 정의하거나 고정값을 줄 수 있음.
        // 여기서는 그냥 false로 고정되어 있음.

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
        long count = cartRepository.count(memberHolder);

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
        long count = cartRepository.count(memberHolder);

        // then
        assertThat(count).isEqualTo(5);
    }

    @Test
    @DisplayName("count: 캐시도 없고 워밍업 대상도 아님 -> 0 반환")
    void count_Zero() {
        // given: Empty Mark 상태
        given(redisRepo.hasKey(memberHolder)).willReturn(false);
        given(redisRepo.isMarkedAsEmpty(1L)).willReturn(true);

        // when
        long count = cartRepository.count(memberHolder);

        // then
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("public warmUp 메소드 테스트 (Eager Loading)")
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