package com.nhnacademy.cart.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cart")
public class CartProperties {
    // 장바구니에 담을 수 있는 '상품 종류' 최대 개수
    private int maxItems = 100;

    //TTL 설정
    private long guestTtlDays = 3;
    private long memberTtlMinutes = 60;

    //스케쥴러 설정
    private int retentionDays = 90;

    private Scheduler scheduler = new Scheduler();
    @Getter
    @Setter
    public static class Scheduler {
        private String cleanupCron = "0 0 3 * * *";
        private int batchSize = 1000;
    }
}