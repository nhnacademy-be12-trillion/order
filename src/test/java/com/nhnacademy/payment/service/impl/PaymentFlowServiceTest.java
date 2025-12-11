package com.nhnacademy.payment.service.impl;

import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.domain.OrderDetails;
import com.nhnacademy.order.order.domain.OrderStatus;
import com.nhnacademy.order.order.exception.OrderNotFoundException;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.payment.config.TossPaymentClient;
import com.nhnacademy.payment.dto.reqeust.PaymentRequestDto;
import com.nhnacademy.payment.dto.response.TossPaymentResponseDto;
import com.nhnacademy.payment.entity.Payment;
import com.nhnacademy.payment.exception.PaymentSaveFailException;
import com.nhnacademy.payment.exception.PaymentStateConflictException;
import com.nhnacademy.payment.exception.PaymentValidationException;
import com.nhnacademy.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentFlowServiceTest {

    @InjectMocks
    private PaymentFlowService paymentFlowService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private TossPaymentClient tossPaymentClient;

    @Mock
    private OrderRepository orderRepository;

    private PaymentRequestDto request;
    private Order order;
    private OrderDetails orderDetails;
    private TossPaymentResponseDto response;

    @BeforeEach
    void setup() {
        request = new PaymentRequestDto("test_paymentKey", "ORD_test", 50000);

        order = new Order();
        ReflectionTestUtils.setField(order, "orderNumber", "ORD_test");
        ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.PENDING);

        orderDetails = mock(OrderDetails.class);
        lenient().when(orderDetails.totalPrice()).thenReturn(50000);
        ReflectionTestUtils.setField(order, "orderDetails", orderDetails);

        response = new TossPaymentResponseDto();
        ReflectionTestUtils.setField(response, "paymentKey", "test_paymentKey");
        ReflectionTestUtils.setField(response, "status", "DONE");
        ReflectionTestUtils.setField(response, "totalAmount", 50000);
    }

    // ==========================================
    // confirmPayment 테스트
    // ==========================================

    @Test
    @DisplayName("결제 승인 성공 - 정상 호출")
    void confirmPayment_Success() {
        // given
        given(orderRepository.findOrderWithItemsByOrderNumber("ORD_test")).willReturn(Optional.of(order));
        given(tossPaymentClient.confirm(any(), any(), any())).willReturn(response);

        // [수정] savePayment는 이제 Payment Entity를 반환합니다.
        Payment expectPayment = mock(Payment.class);
        given(paymentService.savePayment(response, order)).willReturn(expectPayment);

        // when
        Payment result = paymentFlowService.confirmPayment(request);

        // then
        assertEquals(expectPayment, result); // Entity 반환 확인

        verify(orderRepository).findOrderWithItemsByOrderNumber("ORD_test");
        verify(orderDetails).totalPrice();
        verify(tossPaymentClient).confirm("test_paymentKey", "ORD_test", 50000);
        verify(paymentService).savePayment(response, order);
    }

    @Test
    @DisplayName("결제 승인 실패 - (주문 정보 없음)")
    void confirmPayment_Failure_OrderNotFound() {
        given(orderRepository.findOrderWithItemsByOrderNumber("ORD_test")).willReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> paymentFlowService.confirmPayment(request));

        verify(tossPaymentClient, never()).confirm(any(), any(), any());
    }

    @Test
    @DisplayName("결제 승인 실패 - (주문 금액 불일치)")
    void confirmPayment_Failure_AmountMismatch() {
        given(orderDetails.totalPrice()).willReturn(40000); // 금액 다름
        given(orderRepository.findOrderWithItemsByOrderNumber("ORD_test")).willReturn(Optional.of(order));

        assertThrows(PaymentValidationException.class,
                () -> paymentFlowService.confirmPayment(request));

        verify(tossPaymentClient, never()).confirm(any(), any(), any());
    }

    @Test
    @DisplayName("결제 승인 실패 - (이미 결제가 승인된 주문 건)")
    void confirmPayment_Failure_AlreadyCompleted() {
        ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.COMPLETED);
        given(orderRepository.findOrderWithItemsByOrderNumber(request.orderNumber())).willReturn(Optional.of(order));

        assertThrows(PaymentStateConflictException.class,
                () -> paymentFlowService.confirmPayment(request));

        verify(tossPaymentClient, never()).confirm(any(), any(), any());
    }

    @Test
    @DisplayName("결제 승인 실패 - (외부 API 호출 실패)")
    void confirmPayment_Failure_ApiError() {
        given(orderRepository.findOrderWithItemsByOrderNumber(request.orderNumber())).willReturn(Optional.of(order));

        given(tossPaymentClient.confirm(any(), any(), any())).willThrow(
                new RuntimeException("Toss API Network Error")
        );

        assertThatThrownBy(() -> paymentFlowService.confirmPayment(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Toss API Network Error");

        verify(paymentService, never()).savePayment(any(), any());
    }

    @Test
    @DisplayName("결제 승인 실패 - (DB 저장 오류 발생)")
    void confirmPayment_Failure_DbSaveError() {
        given(orderRepository.findOrderWithItemsByOrderNumber(request.orderNumber())).willReturn(Optional.of(order));
        given(tossPaymentClient.confirm(any(), any(), any())).willReturn(response);

        // DB 저장 시 예외 발생
        given(paymentService.savePayment(any(), any())).willThrow(new RuntimeException("DB Error"));

        assertThrows(PaymentSaveFailException.class, () -> paymentFlowService.confirmPayment(request));

        // 롤백 로직 검증
        verify(tossPaymentClient).cancel(request.paymentKey(), "서버 데이터베이스 저장 오류", response.getTotalAmount());
        verify(orderRepository, times(1)).save(order);
        assertEquals(OrderStatus.CANCELED, order.getOrderStatus());
    }

    // ==========================================
    // cancelPayment 테스트
    // ==========================================

    @Test
    @DisplayName("결제 취소 성공 - 정상 호출 (부분 취소 포함)")
    void cancelPayment_Success() {
        // given
        String cancelReason = "단순 변심";
        String paymentKey = "test_paymentKey";
        Integer cancelAmount = 10000;

        Payment payment = mock(Payment.class);
        Order orderMock = mock(Order.class);

        // 주문 상태 통과 (COMPLETED 등)
        given(payment.getOrder()).willReturn(orderMock);
        given(orderMock.getOrderStatus()).willReturn(OrderStatus.COMPLETED);
        given(payment.getPaymentKey()).willReturn(paymentKey);

        // 잔액 설정 (amountToCancel 계산 시 필요하지 않지만 로직상 흐름을 위해)
        // given(payment.getBalanceAmount()).willReturn(50000);

        given(paymentService.getPaymentByOrderNumber(request.orderNumber())).willReturn(payment);

        TossPaymentResponseDto cancelResponse = new TossPaymentResponseDto();
        ReflectionTestUtils.setField(cancelResponse, "status", "CANCELED");

        given(tossPaymentClient.cancel(paymentKey, cancelReason, cancelAmount)).willReturn(cancelResponse);

        // when
        paymentFlowService.cancelPayment(request.orderNumber(), cancelReason, cancelAmount);

        // then
        verify(tossPaymentClient).cancel(paymentKey, cancelReason, cancelAmount);
        verify(paymentService).updatePaymentCanceledStatus(payment, cancelAmount);
    }

    @Test
    @DisplayName("결제 취소 성공 - 금액 null일 때 전액 취소")
    void cancelPayment_Success_FullCancel() {
        // given
        String cancelReason = "전액 취소";
        String paymentKey = "test_paymentKey";
        Integer cancelAmount = null; // null 입력
        int balance = 50000;

        Payment payment = mock(Payment.class);
        Order orderMock = mock(Order.class);

        given(payment.getOrder()).willReturn(orderMock);
        given(orderMock.getOrderStatus()).willReturn(OrderStatus.COMPLETED);
        given(payment.getPaymentKey()).willReturn(paymentKey);
        given(payment.getBalanceAmount()).willReturn(balance); // 잔액 가져오기 호출됨

        given(paymentService.getPaymentByOrderNumber(request.orderNumber())).willReturn(payment);

        TossPaymentResponseDto cancelResponse = new TossPaymentResponseDto();
        ReflectionTestUtils.setField(cancelResponse, "status", "CANCELED");

        given(tossPaymentClient.cancel(paymentKey, cancelReason, balance)).willReturn(cancelResponse);

        // when
        paymentFlowService.cancelPayment(request.orderNumber(), cancelReason, cancelAmount);

        // then
        // null 대신 잔액(balance)이 전달되었는지 확인
        verify(tossPaymentClient).cancel(paymentKey, cancelReason, balance);
        verify(paymentService).updatePaymentCanceledStatus(payment, balance);
    }

    @Test
    @DisplayName("결제 취소 실패 - 이미 취소된 주문 건")
    void cancelPayment_Failure_AlreadyCanceled() {
        String cancelReason = "취소";
        Payment payment = mock(Payment.class);
        Order orderMock = mock(Order.class);

        given(payment.getOrder()).willReturn(orderMock);
        given(orderMock.getOrderStatus()).willReturn(OrderStatus.CANCELED); // [핵심] 이미 취소됨

        given(paymentService.getPaymentByOrderNumber(request.orderNumber())).willReturn(payment);

        assertThrows(PaymentStateConflictException.class,
                () -> paymentFlowService.cancelPayment(request.orderNumber(), cancelReason, 50000));

        verify(tossPaymentClient, never()).cancel(any(), any(), any());
        verify(paymentService, never()).updatePaymentCanceledStatus(any(), any());
    }

    @Test
    @DisplayName("결제 취소 실패 - 결제 대기 상태 (승인 전)")
    void cancelPayment_Failure_Pending() {
        String cancelReason = "취소";
        Payment payment = mock(Payment.class);
        Order orderMock = mock(Order.class);

        given(payment.getOrder()).willReturn(orderMock);
        given(orderMock.getOrderStatus()).willReturn(OrderStatus.PENDING); // [핵심] 결제 전

        given(paymentService.getPaymentByOrderNumber(request.orderNumber())).willReturn(payment);

        assertThrows(PaymentStateConflictException.class,
                () -> paymentFlowService.cancelPayment(request.orderNumber(), cancelReason, 50000));

        verify(tossPaymentClient, never()).cancel(any(), any(), any());
    }

    @Test
    @DisplayName("결제 취소 실패 - 토스 응답 상태 이상 (검증 실패)")
    void cancelPayment_Failure_TossStatusMismatch() {
        String cancelReason = "취소";
        String paymentKey = "key";
        int amount = 10000;

        Payment payment = mock(Payment.class);
        Order orderMock = mock(Order.class);

        given(payment.getOrder()).willReturn(orderMock);
        given(orderMock.getOrderStatus()).willReturn(OrderStatus.COMPLETED);
        given(payment.getPaymentKey()).willReturn(paymentKey);

        given(paymentService.getPaymentByOrderNumber(request.orderNumber())).willReturn(payment);

        // 토스 응답이 CANCELED가 아님
        TossPaymentResponseDto failResponse = new TossPaymentResponseDto();
        ReflectionTestUtils.setField(failResponse, "status", "FAILED");

        given(tossPaymentClient.cancel(paymentKey, cancelReason, amount)).willReturn(failResponse);

        // when
        paymentFlowService.cancelPayment(request.orderNumber(), cancelReason, amount);

        // then
        // DB 업데이트가 호출되지 않아야 함
        verify(paymentService, never()).updatePaymentCanceledStatus(any(), any());
    }
}