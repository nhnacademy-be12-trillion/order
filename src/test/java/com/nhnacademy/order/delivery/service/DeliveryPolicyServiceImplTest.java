package com.nhnacademy.order.delivery.service;

import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.delivery.domain.DeliveryPolicy;
import com.nhnacademy.order.delivery.dto.DeliveryPolicyResponse;
import com.nhnacademy.order.delivery.dto.DeliveryPolicyUpdateRequest;
import com.nhnacademy.order.delivery.exception.PolicyNotConfiguredException;
import com.nhnacademy.order.delivery.repository.DeliveryPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeliveryPolicyServiceImplTest {

    @Mock
    private DeliveryPolicyRepository deliveryPolicyRepository;

    @InjectMocks
    private DeliveryPolicyServiceImpl deliveryPolicyService;

    private DeliveryPolicy deliveryPolicy;

    @BeforeEach
    void setUp() {
        deliveryPolicy = new DeliveryPolicy(1L, 3000, 50000);
    }

    @DisplayName("배송 정책 조회 성공")
    @Test
    void getDeliveryPolicy_Success() {
        // given
        given(deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc()).willReturn(Optional.of(deliveryPolicy));

        // when
        DeliveryPolicyResponse response = deliveryPolicyService.getDeliveryPolicy();

        // then
        assertThat(response.deliveryPolicyId()).isEqualTo(deliveryPolicy.getDeliveryPolicyId());
        assertThat(response.deliveryPolicyFee()).isEqualTo(deliveryPolicy.getDeliveryPolicyFee());
        assertThat(response.deliveryPolicyThreshold()).isEqualTo(deliveryPolicy.getDeliveryPolicyThreshold());
    }

    @DisplayName("배송 정책 조회 실패 - 설정 없음")
    @Test
    void getDeliveryPolicy_Fail_PolicyNotConfigured() {
        // given
        given(deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc()).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deliveryPolicyService.getDeliveryPolicy())
                .isInstanceOf(PolicyNotConfiguredException.class)
                .hasMessage("배송 정책이 설정되지 않음");
    }

    @DisplayName("배송 정책 수정 성공")
    @Test
    void updateDeliveryPolicy_Success() {
        // given
        UserInfo userInfo = new UserInfo(1L, "ADMIN");
        DeliveryPolicyUpdateRequest request = new DeliveryPolicyUpdateRequest(3500, 60000);
        given(deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc()).willReturn(Optional.of(deliveryPolicy));

        // when
        deliveryPolicyService.updateDeliveryPolicy(userInfo, request);

        // then
        verify(deliveryPolicyRepository).save(any(DeliveryPolicy.class));
        assertThat(deliveryPolicy.getDeliveryPolicyFee()).isEqualTo(request.deliveryPolicyFee());
        assertThat(deliveryPolicy.getDeliveryPolicyThreshold()).isEqualTo(request.deliveryPolicyThreshold());
    }

    @DisplayName("배송 정책 수정 실패 - 설정 없음")
    @Test
    void updateDeliveryPolicy_Fail_PolicyNotConfigured() {
        // given
        UserInfo userInfo = new UserInfo(1L, "ADMIN");
        DeliveryPolicyUpdateRequest request = new DeliveryPolicyUpdateRequest(3500, 60000);
        given(deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc()).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deliveryPolicyService.updateDeliveryPolicy(userInfo, request))
                .isInstanceOf(PolicyNotConfiguredException.class)
                .hasMessage("배송 정책이 설정되지 않음");
    }
}
