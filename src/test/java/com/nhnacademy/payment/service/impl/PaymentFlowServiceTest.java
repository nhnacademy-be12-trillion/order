package com.nhnacademy.payment.service.impl;

import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.domain.PaymentStatus;
import com.nhnacademy.order.order.exception.OrderNotFoundException;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.order.service.OrderService;
import com.nhnacademy.payment.config.TossPaymentClient;
import com.nhnacademy.payment.domain.Payment;
import com.nhnacademy.payment.dto.reqeust.PaymentRequestDto;
import com.nhnacademy.payment.dto.response.PaymentResponse;
import com.nhnacademy.payment.dto.response.TossPaymentResponseDto;
import com.nhnacademy.payment.exception.PaymentAlreadyApprovedException;
import com.nhnacademy.payment.exception.PaymentAlreadyCanceledException;
import com.nhnacademy.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.management.relation.Relation;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
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
    private TossPaymentResponseDto response;

    @BeforeEach
    void setup(){
        request = new PaymentRequestDto("test_paymentKey","ORD_test",50000);

        order = new Order();
        ReflectionTestUtils.setField(order, "orderNumber","ORD_test");
        ReflectionTestUtils.setField(order,"paymentStatus", PaymentStatus.PENDING);

        response = new TossPaymentResponseDto();
        ReflectionTestUtils.setField(response, "paymentKey","test_paymentKey");
        ReflectionTestUtils.setField(response,"status","DONE");

    }

    @Test
    @DisplayName("결제 승인 성공  - 정상 호출")
    void confirmPayment_Success()
    {
        given(orderRepository.findByOrderNumber("ORD_test")).willReturn(Optional.of(order));

        given(tossPaymentClient.confirm(any(),any(),any())).willReturn(response);

        PaymentResponse expectResponse = PaymentResponse.builder()
                .status("DONE")
                .build();

        given(paymentService.savePayment(response,order)).willReturn(expectResponse);

        PaymentResponse result = paymentFlowService.confirmPayment(request);

        assertEquals("DONE",result.status());

        verify(orderRepository).findByOrderNumber("ORD_test");
        verify(tossPaymentClient).confirm("test_paymentKey","ORD_test",50000);
        verify(paymentService).savePayment(response,order);
    }

    @Test
    @DisplayName("결제 승인 실패 -(주문 정보 없음)")
    void confirmPayment_Failure()
    {
        given(orderRepository.findByOrderNumber("ORD_test")).willReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,()->paymentFlowService.confirmPayment(request));

        verify(tossPaymentClient, never()).confirm("test_paymentKey","ORD_test",50000);
    }

    @Test
    @DisplayName("결제 승인 실패 - (이미 결제가 승인된 주문 건)")
    void confirmPayment_Failure2(){

        ReflectionTestUtils.setField(order, "paymentStatus", PaymentStatus.COMPLETED);

        given(orderRepository.findByOrderNumber(request.orderNumber())).willReturn(Optional.of(order));

        assertThrows(PaymentAlreadyApprovedException.class,
                () -> paymentFlowService.confirmPayment(request));

        verify(tossPaymentClient,never()).confirm(any(),any(),any());
    }

    @Test
    @DisplayName("결제 승인 실패 - (외부 api 호출 실패)")
    void confirmPayment_Failure3(){
        given(orderRepository.findByOrderNumber(request.orderNumber())).willReturn(Optional.of(order));

        given(tossPaymentClient.confirm(any(),any(),any())).willThrow(
                new RuntimeException("Toss API Network Error")
        );

        assertThatThrownBy(() -> paymentFlowService.confirmPayment(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Toss API Network Error");

        verify(paymentService,never()).savePayment(any(),any());

    }

    @Test
    @DisplayName("결제 승인 실패 - (db 저장 오류 발생)")
     void confirmPayment_Failure4(){
        given(orderRepository.findByOrderNumber(request.orderNumber())).willReturn(Optional.of(order));

        given(tossPaymentClient.confirm(any(),any(),any())).willReturn(response);

        given(paymentService.savePayment(any(),any())).willThrow(
                new RuntimeException("Toss API Network Error"));

        assertThatThrownBy(() -> paymentFlowService.confirmPayment(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("결제 승인 실패");


        verify(tossPaymentClient, times(1))
                .cancel(request.paymentKey(), request.orderNumber());

    }

    @Test
    @DisplayName("결제 취소 성공")
    void cancelPayment_Success()
    {
        String cancelReason = "단순 변심";
        String paymentKey = "test_paymentKey";

        Payment payment = mock(Payment.class);
        given(payment.getPaymentKey()).willReturn(paymentKey);
        given(paymentService.getPaymentByOrderNumber(request.orderNumber())).willReturn(payment);

        Order orderMock = mock(Order.class);
        given(payment.getOrder()).willReturn(orderMock);
        given(orderMock.getPaymentStatus()).willReturn(PaymentStatus.COMPLETED);


        TossPaymentResponseDto cancelResponse = new TossPaymentResponseDto();
        ReflectionTestUtils.setField(cancelResponse, "status", "CANCELED");
        given(tossPaymentClient.cancel(paymentKey, cancelReason)).willReturn(cancelResponse);

        // when
        paymentFlowService.cancelPayment(request.orderNumber(), cancelReason);

        // then
        verify(tossPaymentClient).cancel(paymentKey, cancelReason);
        verify(paymentService).updatePaymentCanceledStatus(payment);

    }

    @Test
    @DisplayName("결제 취소 실패 - 이미 취소된 (상태값)")
    void cancelPayment_Failure(){

        String cancelReason = "단순 변심";

        Payment payment = mock(Payment.class);
        Order orderMock = mock(Order.class);

        given(payment.getOrder()).willReturn(orderMock);
        given(orderMock.getPaymentStatus()).willReturn(PaymentStatus.CANCELED); // [핵심] 이미 취소됨

        given(paymentService.getPaymentByOrderNumber(request.orderNumber())).willReturn(payment);

        assertThrows(PaymentAlreadyCanceledException.class,
                () -> paymentFlowService.cancelPayment(request.orderNumber(), cancelReason));

        verify(tossPaymentClient, never()).cancel(any(), any());

        verify(paymentService, never()).updatePaymentCanceledStatus(any());

    }





}