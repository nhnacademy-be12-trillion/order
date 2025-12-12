package com.nhnacademy.cart.repository.impl;

import com.nhnacademy.cart.common.config.CartProperties;
import com.nhnacademy.cart.domain.RedisCart;
import com.nhnacademy.cart.dto.CartHolder;
import com.nhnacademy.cart.repository.CartRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CartRedisRepositoryImpl implements CartRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CartProperties cartProperties;

    // 변경 감지를 위한 세트
    private static final String DIRTY_MEMBERS_KEY = "cart:dirty:members";

    // 의도적으로 비웠는지, 기타 사유로 비워졌는지 확인하기 위한 세트
    private static final String EMPTY_MEMBERS_KEY = "cart:empty:members";

    // ========================================================
    //  키 생성과 TTL 지정을 위한 헬퍼 메소드
    // ========================================================

    private String generateKey(CartHolder holder) {
        if (holder.isMember()) {
            return "cart:member:" + holder.getMemberId().toString();
        } else {
            return "cart:guest:" + holder.getGuestId();
        }
    }

    private long getTtlInMinutes(CartHolder holder) {
        if (holder.isMember()) {
            return cartProperties.getMemberTtlMinutes();
        } else {
            return TimeUnit.DAYS.toMinutes(cartProperties.getGuestTtlDays());
        }
    }

    // ========================================================
    //  Helper Methods: Dirty & Empty Marking (Write-Back Logic)
    // ========================================================

    /**
     * Dirty Marking: 회원의 장바구니에 변경이 생겼음을 기록
     */
    private void markAsDirty(CartHolder holder) {
        if (holder.isMember()) {
            redisTemplate.opsForSet().add(DIRTY_MEMBERS_KEY, holder.getMemberId().toString());
        }
    }

    /**
     * Empty Marking: 사용자가 의도적으로 장바구니를 비웠음을 기록
     * 현재 시간도 저장하여, 나중에 오래된 마킹을 지울 수 있게 함.
     */

    private void markAsEmpty(CartHolder holder) {
        if (holder.isMember()) {
            redisTemplate.opsForZSet().add(
                    EMPTY_MEMBERS_KEY,
                    holder.getMemberId().toString(),
                    System.currentTimeMillis() // Score = 현재 시간
            );
        }
    }

    /**
     * Dirty Mark Removal:
     * 장바구니가 Empty 마킹이 되어있지 않은데 여러 사유로 캐시(TTL 만료, 일시적인 오류)가 존재하지 않으나,
     * Dirty 마킹으로 남아있을 때... 오류 이므로 삭제해야함.
     * 정상 : (키가 없는 상태 + Empty마킹 + Dirty마킹)
     * 비정상 : (키가 없는 상태 + Dirty마킹)
     */
    private void removeFromDirtySet(CartHolder holder) {
        if (holder.isMember()) {
            redisTemplate.opsForSet().remove(DIRTY_MEMBERS_KEY, holder.getMemberId().toString());
        }
    }

    /**
     * Empty Mark Removal: 여러 사유로 장바구니가 빈 상태가 아니게 되면 마킹 제거
     */
    private void removeFromEmptySet(CartHolder holder) {
        if (holder.isMember()) {
            redisTemplate.opsForZSet().remove(EMPTY_MEMBERS_KEY, holder.getMemberId().toString());}
    }

    // ========================================================
    //  CUD Operations (Modified for Write-Back)
    // ========================================================

    @Override//ok
    public void put(CartHolder holder, RedisCart cart) {
        // 데이터 유효성 체크
        if (cart == null) {
            return;
        }
        String key = generateKey(holder);
        long ttl = getTtlInMinutes(holder);

        redisTemplate.opsForHash().put(key, cart.getBookId().toString(), cart);
        redisTemplate.expire(key, ttl, TimeUnit.MINUTES);

        // Empty 상태 해제 (상품이 들어왔으므로)
        removeFromEmptySet(holder);
        // 변경사항 기록
        markAsDirty(holder);
    }

    @Override//ok
    public void putAll(CartHolder holder, List<RedisCart> carts) {
        // 데이터 유효성 체크
        if (carts == null || carts.isEmpty()) {
            return;
        }

        String key = generateKey(holder);
        long ttl = getTtlInMinutes(holder);

        Map<String, RedisCart> map = carts.stream()
                .collect(Collectors.toMap(
                        c -> String.valueOf(c.getBookId()),
                        c -> c));

        // Redis 저장
        redisTemplate.opsForHash().putAll(key, map);
        redisTemplate.expire(key, ttl, TimeUnit.MINUTES);

        // Empty 상태 해제
        removeFromEmptySet(holder);
        // 변경사항 기록
        markAsDirty(holder);
    }

    /**
     * DB 데이터를 Redis로 Warming합니다.
     */
    @Override
    public void restore(CartHolder holder, List<RedisCart> carts) {
        // [Critical Check] 현재 의도적 삭제 상태인지 확인
        if (holder.isMember() && isMarkedAsEmpty(holder.getMemberId())) {
            return;
        }

        // 빈 상태를 restore 하는 경우 ...
        // 반복적인 DB 조회 -> warm-up 시도를 방지하기 위해, empty 상태 마킹
        if (carts == null || carts.isEmpty()) {
            markAsEmpty(holder);
            // 빈 상태를 원본으로 생각하고 restore 했음 -> Dirty가 아님
            removeFromDirtySet(holder);
            return;
        }

        String key = generateKey(holder);
        long ttl = getTtlInMinutes(holder);

        Map<String, RedisCart> map = carts.stream()
                .collect(Collectors.toMap(
                        c -> String.valueOf(c.getBookId()),
                        c -> c));

        // Redis에 데이터 적재 (DB -> Redis)
        redisTemplate.opsForHash().putAll(key, map);
        redisTemplate.expire(key, ttl, TimeUnit.MINUTES);
        // 캐시가 없어 빈상태인데 Empty 마킹이 되어있지 않고, DirtySet에 등록되어 있다?
        // => DirtySet 에서 삭제... 오류 수정
        removeFromDirtySet(holder);
    }

    @Override
    public void delete(CartHolder holder, Long bookId) {
        String key = generateKey(holder);

        // Redis에서 개별 항목 삭제
        redisTemplate.opsForHash().delete(key, bookId.toString());

        // 변경사항 기록
        markAsDirty(holder);
        if(!hasKey(holder)) // key의 마지막 남은 value를 삭제하여, key가 삭제 됐을 경우 빈상태 기록
            markAsEmpty(holder);
    }

    @Override
    public void deleteAll(CartHolder holder) {
        // Redis에서 전체 항목 삭제
        String key = generateKey(holder);
        redisTemplate.delete(key);

        // 변경사항 기록
        markAsDirty(holder);
        markAsEmpty(holder);
    }

    // ========================================================
    //  READ Operations (Standard Redis Hash Ops)
    // ========================================================

    @Override
    public Optional<RedisCart> findByBookId(CartHolder holder, Long bookId) {
        String key = generateKey(holder);
        Object value = redisTemplate.opsForHash().get(key, bookId.toString());

        return Optional.ofNullable((RedisCart) value);
    }

    @Override
    public List<RedisCart> findAll(CartHolder holder) {
        String key = generateKey(holder);

        return redisTemplate.opsForHash().values(key).stream()
                .map(obj -> (RedisCart) obj)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByBookId(CartHolder holder, Long bookId) {
        String key = generateKey(holder);
        return redisTemplate.opsForHash().hasKey(key, bookId.toString());
    }

    @Override
    public long count(CartHolder holder) {
        String key = generateKey(holder);
        return redisTemplate.opsForHash().size(key);
    }

    @Override
    public boolean hasKey(CartHolder holder) {
        String key = generateKey(holder);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // ========================================================
    //  Scheduler Support Methods
    // ========================================================

    /**
     * 스케줄러가 처리할 Dirty Member ID 목록을 꺼내옵니다. (SPOP)
     */
    @Override
    public List<Object> popDirtyMemberIds(long count) {
        return redisTemplate.opsForSet().pop(DIRTY_MEMBERS_KEY, count);
    }

    /**
     * 해당 회원이 '의도적으로 전체 삭제'를 했는지 확인
     */
    @Override
    public boolean isMarkedAsEmpty(Long memberId) {
        // ZSCORE 명령어로 점수가 조회되면 존재하는 것
        Double score = redisTemplate.opsForZSet().score(EMPTY_MEMBERS_KEY, memberId.toString());
        return score != null;
    }

    /**
     * Empty 마킹을 정리
     */
    @Override
    public void removeEmptyMark(Long memberId) {
        redisTemplate.opsForZSet().remove(EMPTY_MEMBERS_KEY, memberId.toString());
    }


    @Override
    public List<Long> filterCleanMemberIds(Set<Long> memberIds) {
        if (memberIds.isEmpty()) return Collections.emptyList();

        // Object가 아닌 String 리스트로 명확하게 변환
        List<String> memberIdStrings = memberIds.stream()
                .map(Object::toString)
                .collect(Collectors.toList());

        // Pipeline 실행
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            // RedisTemplate의 설정을 그대로 가져옴
            RedisSerializer<String> stringSerializer = redisTemplate.getStringSerializer();

            // Key는 루프 밖에서 한 번만 직렬화
            byte[] rawDirtyKey = stringSerializer.serialize(DIRTY_MEMBERS_KEY);

            for (String memberId : memberIdStrings) {
                byte[] rawMemberId = stringSerializer.serialize(memberId);
                connection.sIsMember(rawDirtyKey, rawMemberId);
            }
            return null;
        });

        // Dirty가 아닌 것만 추출
        List<Long> cleanMemberIds = new ArrayList<>();
        int i = 0;
        for (Long memberId : memberIds) {
            Boolean isDirty = (Boolean) results.get(i++);

            // isDirty가 null이거나 false인 경우 == Dirty Set에 없음 == Clean함
            if (Boolean.FALSE.equals(isDirty)) {
                cleanMemberIds.add(memberId);
            }
        }

        return cleanMemberIds;
    }

    @Override
    public void deleteMembersBatch(Set<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return;

        // Redis Key 목록 생성
        List<String> keys = memberIds.stream()
                .map(id -> "cart:member:" + id)
                .collect(Collectors.toList());

        // Member ID String 목록 생성
        List<String> memberIdStrings = memberIds.stream()
                .map(Object::toString)
                .collect(Collectors.toList());

        // Pipeline 실행 (성능 최적화 & 메타데이터 정리)
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            RedisSerializer<String> stringSerializer = redisTemplate.getStringSerializer();

            // 실제 데이터(Hash Key) 삭제
            for (String key : keys) {
                connection.del(stringSerializer.serialize(key));
            }

            // Set 정리 (Dirty Set & Empty ZSet)
            byte[] rawDirtyKey = stringSerializer.serialize(DIRTY_MEMBERS_KEY);
            byte[] rawEmptyKey = stringSerializer.serialize(EMPTY_MEMBERS_KEY);

            // 한 번에 제거하기 위해 2차원 바이트 배열로 변환
            byte[][] rawMemberIds = memberIdStrings.stream()
                    .map(stringSerializer::serialize)
                    .toArray(byte[][]::new);

            if (rawMemberIds.length > 0) {
                // Dirty Set(Set)에서 제거
                connection.sRem(rawDirtyKey, rawMemberIds);
                // Empty Set(ZSet)에서 제거 (데이터가 삭제되므로 마킹도 삭제)
                connection.zRem(rawEmptyKey, rawMemberIds);
            }
            return null;
        });
    }

    // 스케줄러 재시도 지원용
    @Override
    public void addDirtyMember(Long memberId) {
        redisTemplate.opsForSet().add(DIRTY_MEMBERS_KEY, memberId.toString());
    }

    @Override
    public long removeOldEmptyMarks(double maxScore) {
        // ZSet에서 Score(시간)이 0 ~ maxScore 사이인 멤버들을 일괄 삭제
        Long count = redisTemplate.opsForZSet().removeRangeByScore(EMPTY_MEMBERS_KEY, 0, maxScore);
        return count != null ? count : 0;
    }
}