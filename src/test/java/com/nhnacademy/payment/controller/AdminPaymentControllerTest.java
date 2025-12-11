package com.nhnacademy.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.domain.OrderDetails;
import com.nhnacademy.payment.dto.reqeust.PaymentCancelRequestDto;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "toss.secret-key=test-key")
@AutoConfigureMockMvc
class AdminPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private PaymentFlowService paymentFlowService;

    private Payment payment;
    private final String ORDER_NUMBER = "ORD-123";
    private final Long PAYMENT_ID = 1L;

    @BeforeEach
    void setUp() {
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

        // Payment Entity 생성
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
    @DisplayName("관리자 결제 전체 조회 (페이징) GET - /api/admin/payment")
    void getPayments() throws Exception {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment), pageable, 1);

        given(paymentService.getAllPayments(any(Pageable.class))).willReturn(paymentPage);

        // when & then
        mockMvc.perform(get("/api/admin/payment")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                // AdminPaymentResponse 필드 확인 (paymentId, paymentKey 포함)
                .andExpect(jsonPath("$.content[0].paymentId").value(PAYMENT_ID))
                .andExpect(jsonPath("$.content[0].paymentKey").value("test_key"))
                .andExpect(jsonPath("$.content[0].orderNumber").value(ORDER_NUMBER));
    }

    @Test
    @DisplayName("관리자 결제 단건 조회 (ID) GET - /api/admin/payment/{paymentId}")
    void getPaymentById() throws Exception {
        // given
        given(paymentService.getPaymentById(PAYMENT_ID)).willReturn(payment);

        // when & then
        mockMvc.perform(get("/api/admin/payment/{paymentId}", PAYMENT_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID))
                .andExpect(jsonPath("$.orderNumber").value(ORDER_NUMBER));
    }

    @Test
    @DisplayName("관리자 결제 취소 POST - /api/admin/payment/cancel")
    void cancelPayment() throws Exception {
        // given
        PaymentCancelRequestDto cancelRequest = new PaymentCancelRequestDto(ORDER_NUMBER, "관리자 직권 취소", 50000);

        // when & then
        mockMvc.perform(post("/api/admin/payment/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andDo(print())
                .andExpect(status().isNoContent()); // 204 No Content

        // verify
        verify(paymentFlowService).cancelPayment(
                eq(ORDER_NUMBER),
                eq("관리자 직권 취소"),
                eq(50000)
        );
    }
}