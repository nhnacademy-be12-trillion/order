package com.nhnacademy.cart.repository.impl;

import com.nhnacademy.cart.common.config.CartProperties;
import com.nhnacademy.cart.domain.RedisCart;
import com.nhnacademy.cart.dto.CartHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartRedisRepositoryImplTest {

    @InjectMocks
    private CartRedisRepositoryImpl repository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private CartProperties cartProperties;

    // Redis Operations Mocks
    @Mock private HashOperations<String, Object, Object> hashOps;
    @Mock private SetOperations<String, Object> setOps;
    @Mock private ZSetOperations<String, Object> zSetOps;
    @Mock private RedisSerializer<String> stringSerializer;
    @Mock private RedisSerializer<Object> valueSerializer;

    private CartHolder memberHolder;
    private CartHolder guestHolder;
    private RedisCart sampleCart;

    @BeforeEach
    void setUp() {
        // 기본 Mock 설정
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOps);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOps);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        lenient().when(redisTemplate.getStringSerializer()).thenReturn(stringSerializer);
        lenient().when(redisTemplate.getValueSerializer()).thenReturn((RedisSerializer) valueSerializer);

        // Properties 설정
        lenient().when(cartProperties.getMemberTtlMinutes()).thenReturn(60L);
        lenient().when(cartProperties.getGuestTtlDays()).thenReturn(3L);

        // Test Data
        memberHolder = CartHolder.member(100L);
        guestHolder = CartHolder.guest("guest-123");
        sampleCart = RedisCart.create(1L, 2, LocalDateTime.now());
    }

    @Test
    @DisplayName("PUT: 회원 장바구니 담기 (Dirty O, Empty X)")
    void put_Member() {
        // when
        repository.put(memberHolder, sampleCart);

        // then
        String key = "cart:member:100";
        verify(hashOps).put(eq(key), eq("1"), eq(sampleCart));
        verify(redisTemplate).expire(eq(key), eq(60L), eq(TimeUnit.MINUTES));

        // Empty Mark 제거 (ZSet)
        verify(zSetOps).remove(eq("cart:empty:members"), eq("100"));
        // Dirty Mark 추가 (Set)
        verify(setOps).add(eq("cart:dirty:members"), eq("100"));
    }

    @Test
    @DisplayName("PUT: 비회원 장바구니 담기 (Dirty X, Empty X)")
    void put_Guest() {
        // when
        repository.put(guestHolder, sampleCart);

        // then
        String key = "cart:guest:guest-123";
        long ttlMinutes = TimeUnit.DAYS.toMinutes(3); // 4320
        verify(hashOps).put(eq(key), eq("1"), eq(sampleCart));
        verify(redisTemplate).expire(eq(key), eq(ttlMinutes), eq(TimeUnit.MINUTES));

        // 비회원은 Dirty/Empty 관리 대상이 아님 -> 호출되지 않아야 함
        verify(zSetOps, never()).remove(anyString(), anyString());
        verify(setOps, never()).add(anyString(), anyString());
    }

    @Test
    @DisplayName("PUT: null 입력 시 무시")
    void put_Null() {
        repository.put(memberHolder, null);
        verifyNoInteractions(hashOps);
    }

    @Test
    @DisplayName("PUT ALL: 리스트 저장")
    void putAll() {
        // given
        List<RedisCart> carts = List.of(sampleCart);

        // when
        repository.putAll(memberHolder, carts);

        // then
        verify(hashOps).putAll(eq("cart:member:100"), anyMap());
        verify(zSetOps).remove(anyString(), anyString()); // Empty 해제
        verify(setOps).add(anyString(), anyString()); // Dirty 추가
    }

    @Test
    @DisplayName("PUT ALL: 리스트가 null이거나 비어있으면 무시")
    void putAll_Empty() {
        repository.putAll(memberHolder, null);
        repository.putAll(memberHolder, Collections.emptyList());
        verifyNoInteractions(hashOps);
    }

    @Test
    @DisplayName("RESTORE: 정상 복구 (Dirty 해제)")
    void restore_Success() {
        // given
        List<RedisCart> carts = List.of(sampleCart);
        // Empty 마킹이 안 되어 있다고 가정 (ZSet 조회 결과 null)
        when(zSetOps.score(anyString(), anyString())).thenReturn(null);

        // when
        repository.restore(memberHolder, carts);

        // then
        verify(hashOps).putAll(eq("cart:member:100"), anyMap());
        verify(setOps).remove(eq("cart:dirty:members"), eq("100")); // Dirty 해제 확인
    }

    @Test
    @DisplayName("RESTORE: 이미 Empty로 마킹된 회원은 복구 스킵")
    void restore_SkipIfEmptyMarked() {
        // given
        // Empty 마킹이 되어 있음 (Score 반환됨)
        when(zSetOps.score(anyString(), anyString())).thenReturn(12345.0);

        // when
        repository.restore(memberHolder, List.of(sampleCart));

        // then
        verifyNoInteractions(hashOps); // 저장 시도조차 안 해야 함
    }

    @Test
    @DisplayName("RESTORE: 빈 리스트가 들어오면 Empty 마킹 수행")
    void restore_WithEmptyList() {
        // given
        when(zSetOps.score(anyString(), anyString())).thenReturn(null);

        // when
        repository.restore(memberHolder, Collections.emptyList());

        // then
        // Empty 마킹 (ZSet Add)
        verify(zSetOps).add(eq("cart:empty:members"), eq("100"), anyDouble());
        // Dirty 해제 (Set Remove)
        verify(setOps).remove(eq("cart:dirty:members"), eq("100"));
        // 저장 로직 실행 안 됨
        verifyNoInteractions(hashOps);
    }

    @Test
    @DisplayName("DELETE: 개별 삭제 및 Empty 감지")
    void delete() {
        // given
        String key = "cart:member:100";
        // 삭제 후 키가 존재하지 않는다고 가정 (마지막 아이템 삭제됨)
        when(redisTemplate.hasKey(key)).thenReturn(false);

        // when
        repository.delete(memberHolder, 1L);

        // then
        verify(hashOps).delete(key, "1");
        verify(setOps).add(eq("cart:dirty:members"), eq("100")); // Dirty 마킹
        verify(zSetOps).add(eq("cart:empty:members"), eq("100"), anyDouble()); // Empty 마킹 (키가 사라졌으므로)
    }

    @Test
    @DisplayName("DELETE ALL: 전체 삭제")
    void deleteAll() {
        // when
        repository.deleteAll(memberHolder);

        // then
        verify(redisTemplate).delete("cart:member:100");
        verify(setOps).add(eq("cart:dirty:members"), eq("100"));
        verify(zSetOps).add(eq("cart:empty:members"), eq("100"), anyDouble());
    }

    @Test
    @DisplayName("READ Operations: 단순 조회 위임 확인")
    void readOperations() {
        String key = "cart:member:100";

        // findByBookId
        repository.findByBookId(memberHolder, 1L);
        verify(hashOps).get(key, "1");

        // findAll
        repository.findAll(memberHolder);
        verify(hashOps).values(key);

        // existsByBookId
        repository.existsByBookId(memberHolder, 1L);
        verify(hashOps).hasKey(key, "1");

        // count
        repository.count(memberHolder);
        verify(hashOps).size(key);

        // hasKey
        repository.hasKey(memberHolder);
        verify(redisTemplate).hasKey(key);
    }

    @Test
    @DisplayName("Scheduler Support: popDirtyMemberIds")
    void popDirtyMemberIds() {
        repository.popDirtyMemberIds(100);
        verify(setOps).pop("cart:dirty:members", 100);
    }

    @Test
    @DisplayName("Scheduler Support: Empty Mark 관련")
    void emptyMarkOperations() {
        // isMarkedAsEmpty
        repository.isMarkedAsEmpty(100L);
        verify(zSetOps).score("cart:empty:members", "100");

        // removeEmptyMark
        repository.removeEmptyMark(100L);
        verify(zSetOps).remove("cart:empty:members", "100");

        // removeOldEmptyMarks
        repository.removeOldEmptyMarks(1000.0);
        verify(zSetOps).removeRangeByScore("cart:empty:members", 0, 1000.0);
    }

    @Test
    @DisplayName("Scheduler Support: addDirtyMember")
    void addDirtyMember() {
        repository.addDirtyMember(100L);
        verify(setOps).add("cart:dirty:members", "100");
    }

    // =================================================================
    //  PIPELINE 테스트 (가장 까다로운 부분)
    // =================================================================

    @Test
    @DisplayName("BATCH DELETE: 파이프라인 내부 로직 검증")
    void deleteMembersBatch_Pipeline() {
        // given
        Set<Long> memberIds = Set.of(100L, 200L);
        RedisConnection mockConnection = mock(RedisConnection.class);

        // executePipelined가 호출될 때, 전달된 Callback을 가로채서 실행한다.
        when(redisTemplate.executePipelined(any(RedisCallback.class)))
                .thenAnswer(invocation -> {
                    RedisCallback callback = invocation.getArgument(0);
                    return callback.doInRedis(mockConnection);
                });

        when(stringSerializer.serialize(anyString())).thenReturn(new byte[0]);

        // when
        repository.deleteMembersBatch(memberIds);

        // then
        // 1. Connection의 del 호출 횟수 검증 (회원 수만큼 2번 호출)
        verify(mockConnection, times(2)).del(any(byte[].class));

        // 2. Set 정리 로직 검증 (Dirty Set, Empty ZSet에서 제거)
        // sRem: Dirty Set
        verify(mockConnection).sRem(any(byte[].class), any(byte[][].class));
        // zRem: Empty ZSet
        verify(mockConnection).zRem(any(byte[].class), any(byte[][].class));
    }

    @Test
    @DisplayName("BATCH DELETE: 빈 리스트 입력 시 종료")
    void deleteMembersBatch_Empty() {
        repository.deleteMembersBatch(Collections.emptySet());
        verify(redisTemplate, never()).executePipelined(any(RedisCallback.class));
    }

    @Test
    @DisplayName("FILTER CLEAN: 파이프라인 및 필터링 로직 검증")
    void filterCleanMemberIds_Pipeline() {
        // given
        Set<Long> memberIds = new LinkedHashSet<>(Arrays.asList(100L, 200L, 300L));
        RedisConnection mockConnection = mock(RedisConnection.class);

        // Redis Pipeline 실행 시, 결과 리스트를 가짜로 반환하도록 설정
        // 100L -> True (Dirty), 200L -> False (Clean), 300L -> False (Clean)
        List<Object> pipelineResults = Arrays.asList(true, false, false);

        when(redisTemplate.executePipelined(any(RedisCallback.class)))
                .thenAnswer(invocation -> {
                    RedisCallback callback = invocation.getArgument(0);
                    callback.doInRedis(mockConnection); // 내부 로직(sIsMember 호출) 실행
                    return pipelineResults; // 가짜 결과 반환
                });

        when(stringSerializer.serialize(anyString())).thenReturn(new byte[0]);

        // when
        List<Long> result = repository.filterCleanMemberIds(memberIds);

        // then
        // 1. sIsMember가 3번 호출되었는지 확인
        verify(mockConnection, times(3)).sIsMember(any(byte[].class), any(byte[].class));

        // 2. 결과 필터링 확인 (Dirty인 100은 제외되고, 200, 300만 남아야 함)
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(200L, 300L);
    }

    @Test
    @DisplayName("FILTER CLEAN: 빈 리스트 입력 시 종료")
    void filterCleanMemberIds_Empty() {
        List<Long> result = repository.filterCleanMemberIds(Collections.emptySet());
        assertThat(result).isEmpty();
        verify(redisTemplate, never()).executePipelined(any(RedisCallback.class));
    }
}