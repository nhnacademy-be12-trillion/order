package com.nhnacademy.cart.scheduler;

import com.nhnacademy.cart.domain.EntityCart;
import com.nhnacademy.cart.domain.RedisCart;
import com.nhnacademy.cart.dto.CartHolder;
import com.nhnacademy.cart.repository.CartJpaRepository;
import com.nhnacademy.cart.repository.CartRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartMigrationScheduler {

    private final CartRedisRepository redisRepo;
    private final CartJpaRepository jpaRepo;
    private final PlatformTransactionManager transactionManager; // 트랜잭션 매니저 주입

    // 한 번 가져올 때 Redis에서 꺼낼 회원 수
    private static final int MIGRATION_BATCH_SIZE = 100;

    // 3분 주기일 때 데이터 밀림 방지를 위해 한 번 실행 시 반복할 최대 횟수
    // (100명 * 50회 = 최대 5,000명 처리 후 종료)
    private static final int MAX_MIGRATION_LOOP = 50;

    /**
     * [Write-Back 핵심]
     * Redis의 변경 사항(Dirty)을 DB에 반영합니다.
     * 주기: 3분 (180000ms) - DB 부하를 줄이기 위해 1분 -> 3분으로 조정
     */
    @Scheduled(fixedDelayString = "${cart.scheduler.migration-delay:180000}")
    @SchedulerLock(name = "cart_migration_lock", lockAtMostFor = "180s", lockAtLeastFor = "10s")
    public void syncCartDataToDb() {
        int loopCount = 0;
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        // 루프를 돌며 쌓인 Dirty 데이터를 최대한 소진 (Backlog 처리)
        while (loopCount < MAX_MIGRATION_LOOP) {
            // Dirty Set에서 Pop (꺼내오면서 Redis에서는 사라짐)
            List<Object> dirtyMemberIds = redisRepo.popDirtyMemberIds(MIGRATION_BATCH_SIZE);

            if (dirtyMemberIds == null || dirtyMemberIds.isEmpty()) {
                break; // 더 이상 처리할 회원이 없으면 종료
            }

            loopCount++;
            log.info("[Cart Migration] Batch {}/{} : 대상 회원 {}명 동기화 진행",
                    loopCount, MAX_MIGRATION_LOOP, dirtyMemberIds.size());

            for (Object idObj : dirtyMemberIds) {
                Long memberId = Long.valueOf(String.valueOf(idObj));
                CartHolder holder = CartHolder.member(memberId);

                try {
                    // 트랜잭션 템플릿을 사용하여 명시적으로 트랜잭션 시작
                    // (Self-Invocation 문제 해결: 외부 객체인 TransactionTemplate을 사용하므로 AOP 작동)
                    transactionTemplate.executeWithoutResult(status -> {
                        processMigration(holder);
                    });

                } catch (DataIntegrityViolationException e) {
                    // [Poison Pill 방지] DB 제약조건 위반 등은 재시도해도 해결 안 됨 -> 로그 남기고 Drop
                    log.error("[Cart Migration] CRITICAL: 데이터 무결성 위반으로 Member {} 동기화 중단 (재시도 안함). Cause: {}", memberId, e.getMessage());
                } catch (Exception e) {
                    // [Retry] DB 연결 실패 등 일시적 장애 -> 다시 Dirty Set에 넣어서 다음 턴에 재시도
                    log.error("[Cart Migration] Member {} 일시적 동기화 실패 -> Dirty Set 복구 (다음 턴에 재시도)", memberId, e);
                    redisRepo.addDirtyMember(memberId);
                }
            }

            // DB 스파이크 방지용 미세 대기
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        if (loopCount > 0) {
            log.info("[Cart Migration] 배치 종료. 총 {}번 루프 실행됨.", loopCount);
        }
    }

    /**
     * 실제 동기화 로직 (트랜잭션 안에서 실행됨)
     * 주의: 이 메소드에는 @Transactional을 붙여도 내부 호출이라 무시되므로,
     * 위에서 TransactionTemplate으로 감싸서 호출해야 함.
     */
    protected void processMigration(CartHolder holder) {
        Long memberId = holder.getMemberId();

        // Case 1: 의도적인 전체 삭제 (Empty Mark) 확인
        if (redisRepo.isMarkedAsEmpty(memberId)) {
            jpaRepo.deleteAllByMemberId(memberId); // JPA Repo에 @Modifying @Query 확인 필수
            // DB 반영 완료됐으므로 Redis Empty Mark 제거 (이제 DB도 비었으니 싱크 맞음)
            redisRepo.removeEmptyMark(memberId);
            log.debug("[Cart Migration] Member {} : 장바구니 비움 동기화 완료", memberId);
            return;
        }

        // Case 2: 일반적인 변경 사항 동기화 (Redis -> DB)
        List<RedisCart> redisCarts = redisRepo.findAll(holder);

        // [방어 로직] Redis 데이터 유실 시 DB 보호
        // 상황: Dirty Set에는 감지되었으나, 실제 Redis 장바구니 데이터(Hash)는 비어있음. (TTL 만료 등)
        // 행동: 동기화를 건너뛰어(Skip) DB의 기존 데이터를 보존
        if (redisCarts.isEmpty()) {
            log.warn("[Cart Migration] Member {} : Redis 데이터 유실 감지(TTL/Eviction). DB 삭제를 방지하기 위해 동기화를 건너뜁니다.", memberId);
            return;
        }

        // [Overwrite 전략] 기존 DB 데이터 삭제 후 Redis 데이터로 덮어쓰기
        jpaRepo.deleteAllByMemberId(memberId);

        // RedisCart(String createdAt) -> EntityCart(LocalDateTime createdAt) 변환
        List<EntityCart> entities = redisCarts.stream()
                .map(rc -> EntityCart.builder()
                        .memberId(memberId)
                        .bookId(rc.getBookId())
                        .cartQuantity(rc.getCartQuantity())
                        .createdAt(LocalDateTime.parse(rc.getCreatedAt())) // 파싱 필수
                        .build())
                .collect(Collectors.toList());

        jpaRepo.saveAll(entities);
        log.debug("[Cart Migration] Member {} : {}건 동기화 완료", memberId, entities.size());
    }
}