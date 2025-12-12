package com.nhnacademy.cart.repository.impl;

import com.nhnacademy.cart.domain.EntityCart;
import com.nhnacademy.cart.domain.RedisCart;
import com.nhnacademy.cart.dto.CartDto;
import com.nhnacademy.cart.dto.CartHolder;
import com.nhnacademy.cart.repository.CartJpaRepository;
import com.nhnacademy.cart.repository.CartRedisRepository;
import com.nhnacademy.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CartRepositoryImpl implements CartRepository {

    private final CartJpaRepository jpaRepo;
    private final CartRedisRepository redisRepo;

    // ======================================================================
    //  WRITE Operations: Write-Back Strategy
    //  - 모든 쓰기 작업(CUD)은 Redis에만 즉시 반영합니다.
    //  - Redis Repository 내부에서 'Dirty Marking'을 수행하여,
    //    추후 스케줄러가 DB에 비동기 반영하도록 합니다.
    // ======================================================================

    @Override //ok
    public void save(CartHolder holder, CartDto cartDto) {
        if (cartDto == null) return;

        // DTO -> Redis Entity 변환
        RedisCart redisCart = RedisCart.create(cartDto.getBookId(), cartDto.getCartQuantity(), cartDto.getCreatedAt());

        // Redis 저장 (Dirty Marking 자동 수행)
        redisRepo.put(holder, redisCart);
    }

    @Override //ok
    public void saveAll(CartHolder holder, List<CartDto> cartDtos) {
        if (cartDtos.isEmpty()) return;

        List<RedisCart> redisCarts = cartDtos.stream()
                .map(dto -> RedisCart.create(dto.getBookId(), dto.getCartQuantity(), dto.getCreatedAt()))
                .collect(Collectors.toList());

        // Redis 일괄 저장 (Dirty Marking 자동 수행)
        redisRepo.putAll(holder, redisCarts);
    }

    @Override //ok
    public void delete(CartHolder holder, Long bookId) {
        // Redis 삭제 (Dirty Marking  + Empty Marking 자동 수행)
        redisRepo.delete(holder, bookId);
    }

    @Override //ok
    public void deleteAll(CartHolder holder) {
        // Redis 전체 삭제 (Dirty Marking  + Empty Marking 자동 수행)
        redisRepo.deleteAll(holder);
    }

    // ======================================================================
    //  READ Operations: Look Aside (Optimized)
    //  - 기본 전략: Redis 우선 조회
    //  - Cache Miss: DB 조회 -> Redis 적재(WarmUp) -> ★적재한 데이터 즉시 반환★
    //  - 효율성: WarmUp 직후 다시 Redis를 조회하는 Double Network Trip 방지
    // ======================================================================

    public void warmUp(CartHolder holder){
        tryWarmUpAndGet(holder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartDto> findAll(CartHolder holder) {
        // 워밍 시도 & 데이터 획득
        List<RedisCart> warmedList = tryWarmUpAndGet(holder);

        // 워밍된 데이터가 있다면 바로 반환
        if (warmedList != null) {
            return warmedList.stream()
                    .map(CartDto::fromRedis)
                    .collect(Collectors.toList());
        }

        // 워밍이 안 일어났다면(이미 캐시가 있었다면) Redis 조회
        return redisRepo.findAll(holder).stream()
                .map(CartDto::fromRedis)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CartDto> findByBookId(CartHolder holder, Long bookId) {
        // 워밍 시도 & 데이터 획득
        List<RedisCart> warmedList = tryWarmUpAndGet(holder);

        // 방금 워밍했다면, 메모리 리스트에서 바로 찾음.
        if (warmedList != null) {
            return warmedList.stream()
                    .filter(cart -> cart.getBookId().equals(bookId))
                    .findFirst()
                    .map(CartDto::fromRedis);
        }

        // 기존 캐시가 있었다면 Redis에서 조회
        Optional<RedisCart> cachedCart = redisRepo.findByBookId(holder, bookId);
        return cachedCart.map(CartDto::fromRedis);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByBookId(CartHolder holder, Long bookId) {
        // 워밍 시도 & 데이터 획득
        List<RedisCart> warmedList = tryWarmUpAndGet(holder);

        // 방금 워밍했다면, 메모리 리스트에서 바로 확인
        if (warmedList != null) {
            return warmedList.stream()
                    .anyMatch(cart -> cart.getBookId().equals(bookId));
        }

        // 기존 캐시가 있었다면 Redis에서 확인
        if (redisRepo.hasKey(holder)) {
            return redisRepo.existsByBookId(holder, bookId);
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public long count(CartHolder holder) {
        // 워밍 시도 & 데이터 획득
        List<RedisCart> warmedList = tryWarmUpAndGet(holder);

        // 방금 워밍했다면, 리스트 사이즈를 바로 반환
        if (warmedList != null) {
            return warmedList.size();
        }

        // 기존 캐시가 있었다면 Redis에서 Count
        if (redisRepo.hasKey(holder)) {
            return redisRepo.count(holder);
        }

        return 0;
    }

    // ======================================================================
    //  Internal Helper: Smart Warm-Up
    // ======================================================================

    /**
     * 캐시 워밍(Cache Warming)을 시도하고, 결과물을 반환
     * * @return
     * - List<RedisCart>: 워밍 수행됨. (DB에서 가져온 최신 데이터)
     * - Collections.emptyList(): 워밍 수행됨. (DB가 비어있음)
     * - null: 워밍 수행 안 함. (이미 캐시가 있거나, 워밍이 불필요한 상황)
     */
    private List<RedisCart> tryWarmUpAndGet(CartHolder holder) {
        if(!holder.isMember()) return null; //멤버가 아닌가?... 웜업 불가.
        if(redisRepo.hasKey(holder)) return null; //키가 이미 존재한가?... 웜업 금지!
        if (redisRepo.isMarkedAsEmpty(holder.getMemberId())) return null; //키가 없는 이유가 삭제 때문인가?... 웜업 금지!

        // DB 조회
        List<EntityCart> dbList = jpaRepo.findAllByMemberId(holder.getMemberId());

        // DTO 변환
        List<RedisCart> redisCarts = dbList.stream()
                .map(c -> RedisCart.create(c.getBookId(), c.getCartQuantity(),c.getCreatedAt()))
                .collect(Collectors.toList());

        // 키도 없고 DB에도 없으면, Empty 상태도 마킹... 반복적인 DB 조회 방지용
        redisRepo.restore(holder, redisCarts);

        if (!redisCarts.isEmpty()) {
            return redisCarts;
        } return Collections.emptyList(); // DB에도 없음
    }
}