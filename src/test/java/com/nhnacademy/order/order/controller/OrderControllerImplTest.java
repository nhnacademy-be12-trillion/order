package com.nhnacademy.order.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.order.order.domain.OrdererInfo;
import com.nhnacademy.order.order.domain.OrderStatus;
import com.nhnacademy.order.order.domain.ReceiverInfo;
import com.nhnacademy.order.order.dto.NonMemberOrderGetRequest;
import com.nhnacademy.order.order.dto.OrderCreateRequest;
import com.nhnacademy.order.order.dto.OrderResponse;
import com.nhnacademy.order.order.service.OrderService;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.orderitem.dto.NonMemberOrderItemStatusPatchRequest;
import com.nhnacademy.order.orderitem.dto.OrderItemStatusPatchRequest;
import com.nhnacademy.order.order.dto.NonMemberOrderCancelRequest;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "toss.secret-key=test-key")
@AutoConfigureMockMvc
class OrderControllerImplTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private OrderResponse orderResponse;
    private Page<OrderResponse> orderResponsePage;

    @BeforeEach
    void setUp() {
        OrdererInfo ordererInfo = new OrdererInfo("홍길동", "010-1234-5678");
        ReceiverInfo receiverInfo = new ReceiverInfo("이순신", "010-9876-5432", "서울시 강남구");

        orderResponse = new OrderResponse(
                1L,
                1L,
                "ORD-20251202-12345",
                LocalDateTime.now(),
                OrderStatus.PENDING,
                50000,
                48500,
                2500,
                ordererInfo,
                receiverInfo,
                Collections.emptyList()
        );

        orderResponsePage = new PageImpl<>(List.of(orderResponse), PageRequest.of(0, 10), 1);
    }

    @Test
    @DisplayName("관리자 주문 전체 조회 - GET /api/orders/admin")
    void getAllOrderByAdmin_Success() throws Exception {
        given(orderService.findAllOrders(any(), any(Pageable.class))).willReturn(orderResponsePage);

        mockMvc.perform(get("/api/orders/admin")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "ADMIN")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(1L))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("회원 주문 전체 조회 - GET /api/orders")
    void getAllOrderByCustomer_Success() throws Exception {
        given(orderService.findAllOrderByMemberId(any(), any(Pageable.class))).willReturn(orderResponsePage);

        mockMvc.perform(get("/api/orders")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "MEMBER")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderId").value(1L))
                .andExpect(jsonPath("$.content[0].memberId").value(1L));
    }

    @Test
    @DisplayName("회원 주문 생성 - POST /api/orders")
    void createMemberOrder_Success() throws Exception {
        OrderCreateRequest createRequest = new OrderCreateRequest("홍길동", "010-1234-5678", LocalDateTime.now().plusDays(1), "이순신", "010-9876-5432", "서울시 강남구", "12345", null, 1000, 1L, Collections.emptyList());
        given(orderService.createOrder(any(), any(OrderCreateRequest.class))).willReturn(orderResponse);

        mockMvc.perform(post("/api/orders")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "MEMBER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/orders/1"))
                .andExpect(jsonPath("$.orderId").value(1L));
    }

    @Test
    @DisplayName("회원 주문 단건 조회 - GET /api/orders/{orderId}")
    void getOrderByCustomer_Success() throws Exception {
        given(orderService.findOrderByOrderId(any(), eq(1L))).willReturn(orderResponse);

        mockMvc.perform(get("/api/orders/{orderId}", 1L)
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "MEMBER"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1L));
    }

    @Test
    @DisplayName("비회원 주문 단건 조회 - POST /api/orders/non-members/")
    void getOrderForNonMember_Success() throws Exception {
        NonMemberOrderGetRequest request = new NonMemberOrderGetRequest("ORD-20251202-12345", "password123");
        given(orderService.findOrderByOrderNumber(eq("ORD-20251202-12345"), eq("password123"))).willReturn(orderResponse);

        mockMvc.perform(post("/api/orders/non-members/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-20251202-12345"));
    }

    @Test
    @DisplayName("회원 주문 상품 상태 변경 - PATCH /api/orders/{orderId}/items/{orderItemId}")
    void patchOrderItemStatusByCustomer_Success() throws Exception {
        OrderItemStatusPatchRequest request = new OrderItemStatusPatchRequest(OrderItemStatus.SHIPPED);

        mockMvc.perform(patch("/api/orders/{orderId}/items/{orderItemId}", 1L, 1L)
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "MEMBER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(orderService).patchOrderItemStatus(any(), eq(1L), eq(1L), any(OrderItemStatusPatchRequest.class));
    }

    @Test
    @DisplayName("비회원 주문 상품 상태 변경 - PATCH /api/orders/non-members/{orderId}/items/{orderItemId}")
    void patchOrderItemStatusForNonMember_Success() throws Exception {
        NonMemberOrderItemStatusPatchRequest request = new NonMemberOrderItemStatusPatchRequest("password123", OrderItemStatus.CANCELED);

        mockMvc.perform(patch("/api/orders/non-members/{orderId}/items/{orderItemId}", 1L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(orderService).patchOrderItemStatusForNonMember(eq(1L), eq(1L), any(NonMemberOrderItemStatusPatchRequest.class));
    }

    @Test
    @DisplayName("회원 주문 취소 - DELETE /api/orders/{orderId}")
    void cancelOrder_Success() throws Exception {
        mockMvc.perform(delete("/api/orders/{orderId}", 1L)
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "MEMBER"))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(orderService).cancelOrder(any(), eq(1L));
    }

    @Test
    @DisplayName("비회원 주문 취소 - DELETE /api/orders/non-members/{orderId}")
    void cancelOrderForNonMember_Success() throws Exception {
        NonMemberOrderCancelRequest cancelRequest = new NonMemberOrderCancelRequest("password123");

        mockMvc.perform(delete("/api/orders/non-members/{orderId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(orderService).cancelOrderForNonMember(eq(1L), eq("password123"));
    }
}
