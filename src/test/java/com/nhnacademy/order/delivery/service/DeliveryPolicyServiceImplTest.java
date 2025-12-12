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
        deliveryPolicy = DeliveryPolicy.create(5000, 30000);
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

    @DisplayName("배송 정책 생성 성공 - 설정 없음")
    @Test
    void updateDeliveryPolicy_CreateNew_WhenNotConfigured() {
        // given
        UserInfo userInfo = new UserInfo(1L, "ADMIN");
        DeliveryPolicyUpdateRequest request = new DeliveryPolicyUpdateRequest(3500, 60000);
        given(deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc()).willReturn(Optional.empty());

        // ArgumentCaptor 생성
        org.mockito.ArgumentCaptor<DeliveryPolicy> captor = org.mockito.ArgumentCaptor.forClass(DeliveryPolicy.class);

        // when
        deliveryPolicyService.updateDeliveryPolicy(userInfo, request);

        // then
        // save 메서드에 전달된 DeliveryPolicy 객체를 캡처
        verify(deliveryPolicyRepository).save(captor.capture());
        DeliveryPolicy savedPolicy = captor.getValue();

        // 캡처된 객체의 값을 검증
        assertThat(savedPolicy.getDeliveryPolicyFee()).isEqualTo(request.deliveryPolicyFee());
        assertThat(savedPolicy.getDeliveryPolicyThreshold()).isEqualTo(request.deliveryPolicyThreshold());
    }
}
