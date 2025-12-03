package com.nhnacademy.order.client.service;

import com.nhnacademy.order.client.CouponClient;
import com.nhnacademy.order.client.dto.CouponApplyRequest;
import com.nhnacademy.order.client.dto.CouponGetDiscountAmountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponService 단위 테스트")
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponClient couponClient;

    private UUID testSagaId;
    private Long testMemberId;
    private Long testCouponId;

    @BeforeEach
    void setUp() {
        testSagaId = UUID.randomUUID();
        testMemberId = 1L;
        testCouponId = 100L;
    }

    @Test
    @DisplayName("할인 금액 계산 성공 - 정액 할인")
    void calculateDiscount_Success_FixedAmount() {
        // given
        int originalPrice = 50000;
        int expectedDiscount = 5000;
        when(couponClient.calculateDiscount(any(CouponGetDiscountAmountRequest.class)))
                .thenReturn(expectedDiscount);

        // when
        int actualDiscount = couponService.calculateDiscount(testCouponId, originalPrice);

        // then
        assertThat(actualDiscount).isEqualTo(expectedDiscount);
        
        ArgumentCaptor<CouponGetDiscountAmountRequest> captor = 
                ArgumentCaptor.forClass(CouponGetDiscountAmountRequest.class);
        verify(couponClient, times(1)).calculateDiscount(captor.capture());
        
        CouponGetDiscountAmountRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.couponId()).isEqualTo(testCouponId);
        assertThat(capturedRequest.price()).isEqualTo(originalPrice);
    }

    @ParameterizedTest
    @CsvSource({
            "10000, 1000",
            "50000, 5000",
            "100000, 10000",
            "1000, 100"
    })
    @DisplayName("할인 금액 계산 성공 - 다양한 가격")
    void calculateDiscount_Success_VariousPrices(int price, int discount) {
        // given
        when(couponClient.calculateDiscount(any(CouponGetDiscountAmountRequest.class)))
                .thenReturn(discount);

        // when
        int actualDiscount = couponService.calculateDiscount(testCouponId, price);

        // then
        assertThat(actualDiscount).isEqualTo(discount);
    }

    @Test
    @DisplayName("할인 금액 계산 - 0원 할인")
    void calculateDiscount_ZeroDiscount() {
        // given
        int originalPrice = 5000;
        when(couponClient.calculateDiscount(any(CouponGetDiscountAmountRequest.class)))
                .thenReturn(0);

        // when
        int actualDiscount = couponService.calculateDiscount(testCouponId, originalPrice);

        // then
        assertThat(actualDiscount).isZero();
    }

    @Test
    @DisplayName("할인 금액 계산 실패 - 쿠폰 서비스 오류")
    void calculateDiscount_Failure_CouponServiceError() {
        // given
        when(couponClient.calculateDiscount(any(CouponGetDiscountAmountRequest.class)))
                .thenThrow(new RuntimeException("Coupon service unavailable"));

        // when & then
        assertThatThrownBy(() -> couponService.calculateDiscount(testCouponId, 10000))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Coupon service unavailable");
    }

    @Test
    @DisplayName("쿠폰 적용 성공")
    void applyCoupon_Success() {
        // given
        doNothing().when(couponClient).applyCoupon(any(CouponApplyRequest.class));

        // when
        couponService.applyCoupon(testSagaId, testMemberId, testCouponId);

        // then
        ArgumentCaptor<CouponApplyRequest> captor = 
                ArgumentCaptor.forClass(CouponApplyRequest.class);
        verify(couponClient, times(1)).applyCoupon(captor.capture());
        
        CouponApplyRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.sagaId()).isEqualTo(testSagaId);
        assertThat(capturedRequest.memberId()).isEqualTo(testMemberId);
        assertThat(capturedRequest.couponId()).isEqualTo(testCouponId);
    }

    @Test
    @DisplayName("쿠폰 적용 실패 - 이미 사용된 쿠폰")
    void applyCoupon_Failure_AlreadyUsed() {
        // given
        doThrow(new RuntimeException("Coupon already used"))
                .when(couponClient).applyCoupon(any(CouponApplyRequest.class));

        // when & then
        assertThatThrownBy(() -> 
                couponService.applyCoupon(testSagaId, testMemberId, testCouponId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Coupon already used");
    }

    @Test
    @DisplayName("쿠폰 회수 성공")
    void withdrawCoupon_Success() {
        // given
        doNothing().when(couponClient).withdrawCoupon(any(CouponApplyRequest.class));

        // when
        couponService.withdrawCoupon(testSagaId, testMemberId, testCouponId);

        // then
        ArgumentCaptor<CouponApplyRequest> captor = 
                ArgumentCaptor.forClass(CouponApplyRequest.class);
        verify(couponClient, times(1)).withdrawCoupon(captor.capture());
        
        CouponApplyRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.sagaId()).isEqualTo(testSagaId);
        assertThat(capturedRequest.memberId()).isEqualTo(testMemberId);
        assertThat(capturedRequest.couponId()).isEqualTo(testCouponId);
    }

    @Test
    @DisplayName("쿠폰 회수 실패 - 외부 API 오류")
    void withdrawCoupon_Failure_ExternalError() {
        // given
        doThrow(new RuntimeException("Withdrawal failed"))
                .when(couponClient).withdrawCoupon(any(CouponApplyRequest.class));

        // when & then
        assertThatThrownBy(() -> 
                couponService.withdrawCoupon(testSagaId, testMemberId, testCouponId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Withdrawal failed");
    }

    @Test
    @DisplayName("쿠폰 적용 및 회수 - 동일한 쿠폰")
    void applyCouponAndWithdraw_SameCoupon() {
        // given
        doNothing().when(couponClient).applyCoupon(any(CouponApplyRequest.class));
        doNothing().when(couponClient).withdrawCoupon(any(CouponApplyRequest.class));

        // when
        couponService.applyCoupon(testSagaId, testMemberId, testCouponId);
        couponService.withdrawCoupon(testSagaId, testMemberId, testCouponId);

        // then
        verify(couponClient, times(1)).applyCoupon(any(CouponApplyRequest.class));
        verify(couponClient, times(1)).withdrawCoupon(any(CouponApplyRequest.class));
    }
}