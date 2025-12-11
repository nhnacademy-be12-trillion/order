package com.nhnacademy.payment.service.impl;

import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.domain.OrderDetails;
import com.nhnacademy.order.order.domain.OrderStatus;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.payment.dto.response.TossPaymentResponseDto;
import com.nhnacademy.payment.entity.Payment;
import com.nhnacademy.payment.entity.PaymentStatus;
import com.nhnacademy.payment.exception.PaymentNotFoundException;
import com.nhnacademy.payment.exception.PaymentStateConflictException;
import com.nhnacademy.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;


    private Order order;

    private TossPaymentResponseDto response;

    @BeforeEach
    void setup(){
        order = new Order();
        ReflectionTestUtils.setField(order, "orderId", 1L);
        ReflectionTestUtils.setField(order, "orderNumber", "testOrderNumber");
        order.setOrderStatus(OrderStatus.PENDING);


        OrderDetails mockOrderDetails = mock(OrderDetails.class);

        lenient().when(mockOrderDetails.totalPrice()).thenReturn(50000);

        ReflectionTestUtils.setField(order, "orderDetails", mockOrderDetails);


        // 3. TossPaymentResponseDto 생성
        response = new TossPaymentResponseDto();
        ReflectionTestUtils.setField(response, "paymentKey", "test_paymentKey");
        ReflectionTestUtils.setField(response, "totalAmount", 50000); // [중요] 이거 꼭 있어야 함
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

        Payment result = paymentService.savePayment(response, order);

        // then
        assertNotNull(result);
        assertEquals(PaymentStatus.DONE, result.getPaymentStatus());
        assertEquals(50000, result.getTotalAmount());
        assertEquals(50000, result.getBalanceAmount());

        // 주문 상태 변경 확인
        assertEquals(OrderStatus.COMPLETED, order.getOrderStatus());

        // [확인] savePayment 내부에서 orderRepository.save()가 호출되었는지 검증
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(orderRepository, times(1)).save(any(Order.class));

    }

    @Test
    @DisplayName("결제 취소 상태 업데이트 - 성공")
    void updateCanceledStatus(){
        Long paymentId = 1L;
        Integer cancelAmount = 50000;

        Payment mockPayment = mock(Payment.class);
        given(mockPayment.getPaymentId()).willReturn(paymentId);

        Payment findPayment = Payment.builder()
                .paymentKey("test_paymentKey")
                .paymentStatus(com.nhnacademy.payment.entity.PaymentStatus.DONE)
                .order(order)
                .totalAmount(50000)
                .build();

        ReflectionTestUtils.setField(findPayment, "paymentId", paymentId);

        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(findPayment));

        paymentService.updatePaymentCanceledStatus(mockPayment,cancelAmount);


        // 1. Payment 상태가 CANCELED로 변경되었는지
        assertEquals(PaymentStatus.CANCELED, findPayment.getPaymentStatus());
        // 2. 잔액이 0원인지
        assertEquals(0, findPayment.getBalanceAmount());
        // 3. Order 상태가 CANCELED로 변경되었는지
        assertEquals(OrderStatus.CANCELED, order.getOrderStatus());
    }

    @Test
    @DisplayName("결제 취소 상태 업데이트 - 실패(이미 취소 처리된 결제 정보)")
    void updatedCanceledStatus_Fail2(){
        Long paymentId = 1L;
        Integer cancelAmount = 50000;
        Payment mockPayment = mock(Payment.class);
        given(mockPayment.getPaymentId()).willReturn(paymentId);

        Payment findPayment = Payment.builder()
                .paymentKey("test_paymentKey")
                .paymentStatus(com.nhnacademy.payment.entity.PaymentStatus.CANCELED)
                .order(order)
                .build();

        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(findPayment));

        assertThrows(PaymentStateConflictException.class,()->paymentService.updatePaymentCanceledStatus(mockPayment,cancelAmount));

        assertEquals(com.nhnacademy.payment.entity.PaymentStatus.CANCELED, findPayment.getPaymentStatus());

    }

    @Test
    @DisplayName("결제 취소 상태 업데이트 - 실패(없는 결제 정보)")
    void updateCanceledStatus_Fail(){
        Long paymentId = 1L;
        Integer cancelAmount = 50000;

        Payment mockPayment = mock(Payment.class);
        given(mockPayment.getPaymentId()).willReturn(paymentId);

        given(paymentRepository.findById(mockPayment.getPaymentId())).willReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () ->
                paymentService.updatePaymentCanceledStatus(mockPayment,cancelAmount));

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
                .paymentStatus(com.nhnacademy.payment.entity.PaymentStatus.DONE)
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
                .paymentStatus(com.nhnacademy.payment.entity.PaymentStatus.DONE)
                .order(order)
                .build();

        ReflectionTestUtils.setField(payment,"paymentId",paymentId);

        given(paymentRepository.findById(paymentId)).willReturn(Optional.of(payment));

        Payment result = paymentService.getPaymentById(paymentId);
        assertNotNull(result);
        // Entity Getter 사용
        assertEquals("test_paymentKey", result.getPaymentKey());
        assertEquals(1L, result.getPaymentId());
    }

    @Test
    @DisplayName("결제 내역 전체 조회 - 성공")
    void getAllPaymentsSuccess() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // setup()에서 이미 생성된 order(mockOrderDetails 포함)를 사용하여 Payment 객체 생성
        Payment payment = Payment.builder()
                .paymentKey("test_paymentKey")
                .paymentStatus(com.nhnacademy.payment.entity.PaymentStatus.DONE)
                .order(order) // [중요] setup에서 만든 order 사용
                .totalAmount(50000)
                .build();

        ReflectionTestUtils.setField(payment, "paymentId", 1L);

        // Repository가 반환할 Page 객체 생성
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment));

        given(paymentRepository.findAll(pageable)).willReturn(paymentPage);

        // when
        Page<Payment> result = paymentService.getAllPayments(pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        // Entity Getter 사용
        assertEquals("test_paymentKey", result.getContent().get(0).getPaymentKey());
        assertEquals(50000, result.getContent().get(0).getTotalAmount());

        verify(paymentRepository, times(1)).findAll(pageable);
    }

    
}
