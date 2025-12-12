package com.nhnacademy.cart.scheduler;

import com.nhnacademy.cart.common.config.CartProperties;
import com.nhnacademy.cart.repository.CartJpaRepository;
import com.nhnacademy.cart.repository.CartRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartCleanupScheduler {

    private final CartJpaRepository cartJpaRepository;
    private final CartRedisRepository cartRedisRepository;
    private final CartProperties cartProperties; // 설정 파일 주입
    private final PlatformTransactionManager transactionManager;

    // 무한 루프 방지를 위한 최대 반복 횟수 (안전장치)
    private static final int MAX_BATCH_LOOP = 500;

    /**
     * 오래된 장바구니 아이템 삭제 스케줄러
     * - 설정된 Cron 시간에 실행
     * - ShedLock을 사용하여 분산 환경에서 중복 실행 방지
     */
    @Scheduled(cron = "#{@cartProperties.scheduler.cleanupCron}")
    @SchedulerLock(name = "cart_item_cleanup_lock", lockAtMostFor = "20m", lockAtLeastFor = "1m")
    public void cleanupOldCartItems() {
        long startTime = System.currentTimeMillis();

        // 프로퍼티에서 설정값 로드
        int retentionDays = cartProperties.getRetentionDays();
        int batchSize = cartProperties.getScheduler().getBatchSize();

        // 삭제 기준 시간 계산 (현재 - 설정값 일)
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);

        log.info("[Cart Cleanup] 시작: {}일 이상 지난 아이템 정리 (기준: {})", retentionDays, threshold);

        int totalDeletedCount = 0;
        int loopCount = 0;
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        while (loopCount < MAX_BATCH_LOOP) {
            loopCount++;

            // 트랜잭션 범위: 조회 -> 필터링 -> DB삭제 -> (커밋 후) Redis 삭제
            Integer deletedInBatch = transactionTemplate.execute(status -> {
                // [DB 조회] 삭제 대상 후보 조회 (Limit 적용)
                Pageable pageable = PageRequest.of(0, batchSize);
                List<Object[]> targets = cartJpaRepository.findOldItemIdsAndMemberIds(threshold, pageable);

                if (targets.isEmpty()) {
                    return 0; // 더 이상 삭제할 데이터 없음
                }

                // [필터링] Redis Dirty Checking
                // 현재 활동 중(Dirty)인 회원의 데이터는 건드리지 않음 (데이터 꼬임 방지)
                Set<Long> candidateMemberIds = targets.stream()
                        .map(row -> (Long) row[1])
                        .collect(Collectors.toSet());

                // Dirty하지 않은(Clean) 회원만 선별
                List<Long> cleanMemberIds = cartRedisRepository.filterCleanMemberIds(candidateMemberIds);

                if (cleanMemberIds.isEmpty()) {
                    log.debug("[Cart Cleanup] 조회된 {}건 모두 활동 중인 회원 데이터라 스킵함.", targets.size());
                    return 0;
                }

                // Clean 회원에 해당하는 Cart ID만 추출
                List<Long> targetCartIds = targets.stream()
                        .filter(row -> cleanMemberIds.contains((Long) row[1]))
                        .map(row -> (Long) row[0])
                        .collect(Collectors.toList());

                Set<Long> targetMemberIds = new HashSet<>(cleanMemberIds);

                // [DB 삭제]
                int count = cartJpaRepository.deleteByCartIds(targetCartIds);

                // [Redis Eviction]
                // 해당 회원의 Redis 캐시를 날림 -> 다음 조회 시 DB(삭제된 상태)에서 새로 로딩됨(WarmUp)
                executeAfterCommit(() -> {
                    try {
                        cartRedisRepository.deleteMembersBatch(targetMemberIds);
                        log.debug("[Redis] 회원 {}명 캐시 갱신(삭제) 완료", targetMemberIds.size());
                    } catch (Exception e) {
                        log.error("[Redis] 캐시 삭제 실패 (DB 정합성 일시 불일치 가능) - {}", e.getMessage());
                    }
                });

                return count;
            });

            // 종료 조건: 삭제된 건수가 0이면 루프 탈출
            if (deletedInBatch == null || deletedInBatch == 0) {
                break;
            }

            totalDeletedCount += deletedInBatch;

            // [DB 부하 조절]
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("[Cart Cleanup] 종료 - 총 삭제 아이템: {}건 (소요시간: {}ms)", totalDeletedCount, duration);
    }

    /**
     * 매일 새벽 4시에 실행하여, 3일 이상 지난 Empty Mark를 제거합니다.
     * (Empty Mark: 장바구니를 일부러 비웠다는 표시)
     */
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(name = "cart_empty_mark_cleanup_lock", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void cleanupOldEmptyMarks() {
        log.info("[Redis Cleanup] 오래된 Empty Mark 정리 시작");

        // 기준: 3일
        // 이 기간 동안 한 번도 장바구니에 접근하지 않았다면, Empty Mark를 지워도 무방함.
        // (다음에 오면 DB 한 번 조회하고 다시 Mark가 생길 테니까)
        int retentionDays = 3;
        long retentionMillis = TimeUnit.DAYS.toMillis(retentionDays);

        // 현재 시간 - 3일 = 삭제 기준 시간 (Score)
        double maxScore = System.currentTimeMillis() - retentionMillis;

        try {
            long deletedCount = cartRedisRepository.removeOldEmptyMarks(maxScore);
            if (deletedCount > 0) {
                log.info("[Redis Cleanup] 3일 지난 장바구니 Empty Mark {}건 삭제 완료 (메모리 확보)", deletedCount);
            } else {
                log.info("[Redis Cleanup] 삭제할 Empty Mark 없음");
            }
        } catch (Exception e) {
            log.error("[Redis Cleanup] Empty Mark 삭제 중 오류 발생", e);
        }
    }

    /**
     * 트랜잭션이 성공적으로 커밋된 후에만 실행하도록 보장하는 헬퍼 메서드
     */
    private void executeAfterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
        } else {
            runnable.run();
        }
    }
}