package com.nhnacademy.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.domain.OrderDetails;
import com.nhnacademy.payment.dto.reqeust.PaymentCancelRequestDto;
import com.nhnacademy.payment.dto.reqeust.PaymentRequestDto;
import com.nhnacademy.payment.entity.Payment;
import com.nhnacademy.payment.entity.PaymentStatus;
import com.nhnacademy.payment.service.PaymentService;
import com.nhnacademy.payment.service.impl.PaymentFlowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "toss.secret-key=test-key")
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private PaymentFlowService paymentFlowService;

    private PaymentRequestDto request;
    private Payment payment;
    private final String ORDER_NUMBER = "ORD-123";
    private final Long PAYMENT_ID = 1L;

    @BeforeEach
    void setUp() {
        request = new PaymentRequestDto("test_key", ORDER_NUMBER, 50000);

        // Order Mock 생성
        Order order = new Order();
        ReflectionTestUtils.setField(order, "orderNumber", ORDER_NUMBER);
        OrderDetails orderDetails = new OrderDetails(
                LocalDateTime.now(), // orderDate
                "12345",             // shippingPostCode (우편번호 더미)
                LocalDateTime.now().plusDays(3), // deliveryDate (배송일 더미)
                0,                   // deliveryFee (배송비)
                0,                   // pointUsage (포인트 사용)
                50000,               // originPrice (원가)
                50000,               // totalPrice (최종가)
                null                 // couponId (쿠폰 없으면 null)
        );
        ReflectionTestUtils.setField(order, "orderDetails", orderDetails);

        // Payment Entity 생성 (Service가 Entity를 반환하므로)
        payment = Payment.builder()
                .paymentKey("test_key")
                .paymentStatus(PaymentStatus.DONE)
                .order(order)
                .totalAmount(50000)
                .balanceAmount(50000)
                .paymentRequestAt(LocalDateTime.now())
                .paymentApprovedAt(LocalDateTime.now())
                .build();

        ReflectionTestUtils.setField(payment, "paymentId", PAYMENT_ID);
    }

    @Test
    @DisplayName("결제 승인 성공 테스트 POST - /api/payment/confirm")
    void confirmPaymentSuccess() throws Exception {
        // given: Service는 Payment Entity를 반환함
        given(paymentFlowService.confirmPayment(any(PaymentRequestDto.class)))
                .willReturn(payment);

        // when & then
        mockMvc.perform(post("/api/payment/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(ORDER_NUMBER)) // DTO 변환 확인
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.totalAmount").value(50000));
    }

    @Test
    @DisplayName("결제 단건 조회 (주문번호) GET - /api/payment/{orderNumber}")
    void getPaymentByOrderNumber() throws Exception {
        // given
        given(paymentService.getPaymentByOrderNumber(ORDER_NUMBER))
                .willReturn(payment);

        // when & then
        mockMvc.perform(get("/api/payment/{orderNumber}", ORDER_NUMBER))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(ORDER_NUMBER))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    @DisplayName("결제 취소 요청 테스트 POST - /api/payment/cancel")
    void cancelPayment() throws Exception {
        // given
        PaymentCancelRequestDto cancelRequest = new PaymentCancelRequestDto(ORDER_NUMBER, "단순 변심", 30000);

        // when & then
        mockMvc.perform(post("/api/payment/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("결제 취소 요청이 정상적으로 처리되었습니다."));

        // verify
        verify(paymentFlowService).cancelPayment(
                eq(ORDER_NUMBER),
                eq("단순 변심"),
                eq(30000)
        );
    }

    @Test
    @DisplayName("결제 전체 조회 (페이징) GET - /api/payment")
    void getPayments() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        // Service는 Page<Payment>를 반환함
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment), pageable, 1);

        given(paymentService.getAllPayments(any(Pageable.class))).willReturn(paymentPage);

        // when & then
        mockMvc.perform(get("/api/payment")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").value(ORDER_NUMBER))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}