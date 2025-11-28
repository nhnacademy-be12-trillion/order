package com.nhnacademy.payment.service.impl;

import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.domain.OrderDetails;
import com.nhnacademy.order.order.domain.PaymentStatus;
import com.nhnacademy.order.order.service.OrderService;
import com.nhnacademy.payment.domain.Payment;
import com.nhnacademy.payment.dto.response.PaymentResponse;
import com.nhnacademy.payment.dto.response.TossPaymentResponseDto;
import com.nhnacademy.payment.exception.PaymentAlreadyCanceledException;
import com.nhnacademy.payment.exception.PaymentNotFoundException;
import com.nhnacademy.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    OrderService orderService;

    private Order order;

    private TossPaymentResponseDto response;

    @BeforeEach
    void setup(){
        order = new Order();

        ReflectionTestUtils.setField(order,"orderId",1L);
        ReflectionTestUtils.setField(order,"orderNumber","testOrderNumber");

        OrderDetails orderDetails = mock(OrderDetails.class);
        lenient().when(orderDetails.totalPrice()).thenReturn(50000); // totalPrice 호출 시 50000원 리턴하도록 설정
        ReflectionTestUtils.setField(order, "orderDetails", orderDetails);

        // Order 객체에 orderDetails 필드 강제 주입
        response = new TossPaymentResponseDto();

        ReflectionTestUtils.setField(response,"paymentKey","test_paymentKey");
        ReflectionTestUtils.setField(response, "requestedAt", "2024-11-25T10:00:00+09:00");
        ReflectionTestUtils.setField(response, "approvedAt", "2024-11-25T10:00:05+09:00");

        TossPaymentResponseDto.Receipt receipt = new TossPaymentResponseDto.Receipt();
        ReflectionTestUtils.setField(receipt, "url", "http://receipt.url");
        ReflectionTestUtils.setField(response, "receipt", receipt);

    }


    @Test
    @DisplayName("결제 저장 테스트 - 승인 성공시")
    void savePaymentSuccess(){

        given(paymentRepository.save(any(Payment.class))).willAnswer(
                invocationOnMock -> {
                    Payment payment = invocationOnMock.getArgument(0);
                    ReflectionTestUtils.setField(payment,"paymentId",1L);
                    return payment;
                }
        );

        PaymentResponse result = paymentService.savePayment(response,order);

        assertNotNull(result);
        assertEquals("DONE",result.status());
        assertEquals(50000,result.totalAmount());

        verify(paymentRepository,times(1)).save(any(Payment.class));


    }

    @Test
    @DisplayName("결제 취소 상태 업데이트 - 성공")
    void updateCanceledStatus(){
        Long paymentId = 1L;

        Payment mockPayment = mock(Payment.class);
        given(mockPayment.getPaymentId()).willReturn(paymentId);

        Payment findPayment = Payment.builder()
                .paymentKey("test_paymentKey")
                .paymentStatus(com.nhnacademy.payment.domain.PaymentStatus.DONE)
                .order(order)
                .build();

        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(findPayment));

        paymentService.updatePaymentCanceledStatus(mockPayment);


        assertEquals(com.nhnacademy.payment.domain.PaymentStatus.CANCELED, findPayment.getPaymentStatus());
        assertEquals(PaymentStatus.CANCELED, order.getPaymentStatus());
    }

    @Test
    @DisplayName("결제 취소 상태 업데이트 - 실패(이미 취소 처리된 결제 정보)")
    void updatedCanceledStatus_Fail2(){
        Long paymentId = 1L;
        Payment mockPayment = mock(Payment.class);
        given(mockPayment.getPaymentId()).willReturn(paymentId);

        Payment findPayment = Payment.builder()
                .paymentKey("test_paymentKey")
                .paymentStatus(com.nhnacademy.payment.domain.PaymentStatus.CANCELED)
                .order(order)
                .build();

        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(findPayment));

        assertThrows(PaymentAlreadyCanceledException.class,()->paymentService.updatePaymentCanceledStatus(mockPayment));

        assertEquals(com.nhnacademy.payment.domain.PaymentStatus.CANCELED, findPayment.getPaymentStatus());

    }

    @Test
    @DisplayName("결제 취소 상태 업데이트 - 실패(없는 결제 정보)")
    void updateCanceledStatus_Fail(){
        Long paymentId = 1L;

        Payment mockPayment = mock(Payment.class);
        given(mockPayment.getPaymentId()).willReturn(paymentId);

        given(paymentRepository.findById(mockPayment.getPaymentId())).willReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () ->
                paymentService.updatePaymentCanceledStatus(mockPayment));

    }



    @Test
    @DisplayName("주문 번호로 결제 내역 조회 - 실패(조회 내역 없음)")
    void getPaymentByOrderNumberFail(){
        String orderNumber = "non-existNumber";
        given(paymentRepository.findByOrder_OrderNumber(orderNumber)).willReturn(null);

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentByOrderNumber(orderNumber));
    }

    @Test
    @DisplayName("결제 ID로 조회 - 실패(결제 내역 정보 없음)")
    public void getPaymentByPaymentIdFail(){

        Long invalidPaymentId = 999L;


        given(paymentRepository.findById(invalidPaymentId)).willReturn(Optional.empty());


        assertThrows(PaymentNotFoundException.class, () ->
                paymentService.getPaymentById(invalidPaymentId));
    }

    @Test
    @DisplayName("주문 번호로 결제 내역 조회 - 성공")
    public void getPaymentByOrderNumberSuccess(){
        String orderNumber = "testOrderNumber";
        Payment payment = Payment.builder()
                .paymentKey("test_paymentKey")
                .paymentStatus(com.nhnacademy.payment.domain.PaymentStatus.DONE)
                .order(order)
                .build();
        given(paymentRepository.findByOrder_OrderNumber(orderNumber)).willReturn(payment);

        Payment result = paymentService.getPaymentByOrderNumber(orderNumber);
        assertNotNull(result);
        assertEquals("test_paymentKey", result.getPaymentKey());
    }

    @Test
    @DisplayName("결제 Id로 주문 조회 - 성공")
    public void getPaymentByPaymentIdSuccess(){
        Long paymentId = 1L;

        Payment payment = Payment.builder()
                .paymentKey("test_paymentKey")
                .paymentStatus(com.nhnacademy.payment.domain.PaymentStatus.DONE)
                .order(order)
                .build();

        ReflectionTestUtils.setField(payment,"paymentId",paymentId);

        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

        PaymentResponse result = paymentService.getPaymentById(paymentId);

        assertNotNull(result);
        assertEquals("test_paymentKey",result.paymentKey());
        assertEquals(1L,result.paymentId());
    }
}
