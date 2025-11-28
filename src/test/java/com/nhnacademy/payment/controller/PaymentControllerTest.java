/*
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 * + Copyright 2025. NHN Academy Corp. All rights reserved.
 * + * While every precaution has been taken in the preparation of this resource,  assumes no
 * + responsibility for errors or omissions, or for damages resulting from the use of the information
 * + contained herein
 * + No part of this resource may be reproduced, stored in a retrieval system, or transmitted, in any
 * + form or by any means, electronic, mechanical, photocopying, recording, or otherwise, without the
 * + prior written permission.
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 */

package com.nhnacademy.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.payment.dto.reqeust.PaymentRequestDto;
import com.nhnacademy.payment.dto.response.PaymentResponse;
import com.nhnacademy.payment.service.PaymentService;
import com.nhnacademy.payment.service.impl.PaymentFlowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
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
    private PaymentResponse response;
    private final String ORDER_NUMBER = "ORD-123";
    private final Long PAYMENT_ID = 1L;

    @BeforeEach
    void setUp() {
        request = new PaymentRequestDto("test_key",ORDER_NUMBER,50000);

        response = PaymentResponse.builder()
                .paymentId(PAYMENT_ID)
                .orderNumber(ORDER_NUMBER)
                .totalAmount(50000)
                .status("DONE")
                .build();

    }

    @Test
    @DisplayName("결제 승인 성공 테스트 POST - /payments/success")
    void createConfirmPaymentSuccess() throws Exception {
        given(paymentFlowService.confirmPayment(any(PaymentRequestDto.class)))
                .willReturn(response);

        mockMvc.perform(post("/payments/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(ORDER_NUMBER))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.totalAmount").value(50000));
    }

    @Test
    @DisplayName("결제 단건 조회 테스트 GET - /payments/{paymentId}")
    void getPaymentById()throws Exception {

        given(paymentService.getPaymentById(PAYMENT_ID))
                .willReturn(response);

        mockMvc.perform(get("/payments/{paymentId}", PAYMENT_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(ORDER_NUMBER))
                .andExpect(jsonPath("$.paymentId").value(PAYMENT_ID));
    }

    @Test
    @DisplayName("결제 정상 취소 테스트 POST - /payments/cancel")
    void cancelPayment() throws Exception {
        Map<String, String> cancelRequestBody = new HashMap<>();
        cancelRequestBody.put("orderNumber", ORDER_NUMBER);
        cancelRequestBody.put("cancelReason","단순 변심");

        mockMvc.perform(post("/payments/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cancelRequestBody)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("결제 취소 완료"));

        verify(paymentFlowService).cancelPayment(eq(ORDER_NUMBER), eq("단순 변심"));
    }

}