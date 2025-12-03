package com.nhnacademy.order.client.service;

import com.nhnacademy.order.client.MemberClient;
import com.nhnacademy.order.client.dto.PointUsageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberService 단위 테스트")
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberClient memberClient;

    private UUID testSagaId;
    private Long testMemberId;

    @BeforeEach
    void setUp() {
        testSagaId = UUID.randomUUID();
        testMemberId = 1L;
    }

    @Test
    @DisplayName("포인트 감소 성공")
    void decreasePoint_Success() {
        // given
        int pointToDecrease = 1000;
        doNothing().when(memberClient).decreasePoint(any(PointUsageRequest.class));

        // when
        memberService.decreasePoint(testSagaId, testMemberId, pointToDecrease);

        // then
        ArgumentCaptor<PointUsageRequest> captor = 
                ArgumentCaptor.forClass(PointUsageRequest.class);
        verify(memberClient, times(1)).decreasePoint(captor.capture());
        
        PointUsageRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.sagaId()).isEqualTo(testSagaId);
        assertThat(capturedRequest.memberId()).isEqualTo(testMemberId);
        assertThat(capturedRequest.point()).isEqualTo(pointToDecrease);
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 500, 1000, 5000, 10000})
    @DisplayName("포인트 감소 성공 - 다양한 포인트 값")
    void decreasePoint_Success_VariousAmounts(int point) {
        // given
        doNothing().when(memberClient).decreasePoint(any(PointUsageRequest.class));

        // when
        memberService.decreasePoint(testSagaId, testMemberId, point);

        // then
        ArgumentCaptor<PointUsageRequest> captor = 
                ArgumentCaptor.forClass(PointUsageRequest.class);
        verify(memberClient).decreasePoint(captor.capture());
        assertThat(captor.getValue().point()).isEqualTo(point);
    }

    @Test
    @DisplayName("포인트 감소 실패 - 잔액 부족")
    void decreasePoint_Failure_InsufficientBalance() {
        // given
        int largePoint = 100000;
        doThrow(new RuntimeException("Insufficient point balance"))
                .when(memberClient).decreasePoint(any(PointUsageRequest.class));

        // when & then
        assertThatThrownBy(() -> 
                memberService.decreasePoint(testSagaId, testMemberId, largePoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient point balance");
    }

    @Test
    @DisplayName("포인트 증가 성공")
    void increasePoint_Success() {
        // given
        int pointToIncrease = 2000;
        doNothing().when(memberClient).increasePoint(any(PointUsageRequest.class));

        // when
        memberService.increasePoint(testSagaId, testMemberId, pointToIncrease);

        // then
        ArgumentCaptor<PointUsageRequest> captor = 
                ArgumentCaptor.forClass(PointUsageRequest.class);
        verify(memberClient, times(1)).increasePoint(captor.capture());
        
        PointUsageRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.sagaId()).isEqualTo(testSagaId);
        assertThat(capturedRequest.memberId()).isEqualTo(testMemberId);
        assertThat(capturedRequest.point()).isEqualTo(pointToIncrease);
    }

    @Test
    @DisplayName("포인트 증가 실패 - 외부 API 오류")
    void increasePoint_Failure_ExternalError() {
        // given
        doThrow(new RuntimeException("Point increase failed"))
                .when(memberClient).increasePoint(any(PointUsageRequest.class));

        // when & then
        assertThatThrownBy(() -> 
                memberService.increasePoint(testSagaId, testMemberId, 1000))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Point increase failed");
    }

    @Test
    @DisplayName("포인트 감소 후 증가 - 보상 트랜잭션 시뮬레이션")
    void decreaseAndIncreasePoint_CompensationScenario() {
        // given
        int point = 1000;
        doNothing().when(memberClient).decreasePoint(any(PointUsageRequest.class));
        doNothing().when(memberClient).increasePoint(any(PointUsageRequest.class));

        // when
        memberService.decreasePoint(testSagaId, testMemberId, point);
        memberService.increasePoint(testSagaId, testMemberId, point);

        // then
        verify(memberClient, times(1)).decreasePoint(any(PointUsageRequest.class));
        verify(memberClient, times(1)).increasePoint(any(PointUsageRequest.class));
    }
}