package com.nhnacademy.order.delivery.repository;

import com.nhnacademy.order.delivery.domain.DeliveryPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class DeliveryPolicyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DeliveryPolicyRepository deliveryPolicyRepository;

    @Test
    @DisplayName("배송비 정책 조회 - 성공 (정책은 반드시 하나만 존재)")
    void findFirstByOrderByDeliveryPolicyIdAsc_Success_WithSinglePolicy() {
        // given: 단 하나의 정책만 저장
        DeliveryPolicy policy = new DeliveryPolicy(null, 5000, 30000);
        entityManager.persistAndFlush(policy);
        entityManager.clear();

        // when
        Optional<DeliveryPolicy> resultOptional = deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc();

        // then
        assertThat(resultOptional).isPresent();
        assertThat(resultOptional.get().getDeliveryPolicyFee()).isEqualTo(5000);
    }
}
