package com.nhnacademy.order.delivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.order.delivery.dto.DeliveryPolicyResponse;
import com.nhnacademy.order.delivery.dto.DeliveryPolicyUpdateRequest;
import com.nhnacademy.order.delivery.exception.PolicyNotConfiguredException;
import com.nhnacademy.order.delivery.service.DeliveryPolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "toss.secret-key=test-key")
@AutoConfigureMockMvc
class DeliveryControllerImplTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeliveryPolicyService deliveryPolicyService;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("배송 정책 조회 성공")
    @Test
    void getDeliveryPolicy_Success() throws Exception {
        // given
        DeliveryPolicyResponse response = new DeliveryPolicyResponse(1L, 3000, 50000);
        given(deliveryPolicyService.getDeliveryPolicy()).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/orders/delivery-policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryPolicyId").value(1L))
                .andExpect(jsonPath("$.deliveryPolicyFee").value(3000))
                .andExpect(jsonPath("$.deliveryPolicyThreshold").value(50000));
    }

    @DisplayName("배송 정책 조회 실패 - 설정 없음")
    @Test
    void getDeliveryPolicy_Fail_PolicyNotConfigured() throws Exception {
        // given
        given(deliveryPolicyService.getDeliveryPolicy()).willThrow(new PolicyNotConfiguredException("배송 정책이 설정되지 않음"));

        // when & then
        mockMvc.perform(get("/api/orders/delivery-policy"))
                .andExpect(status().isNotFound());
    }

    @DisplayName("배송 정책 수정 성공")
    @Test
    void updateDeliveryPolicy_Success() throws Exception {
        // given
        DeliveryPolicyUpdateRequest request = new DeliveryPolicyUpdateRequest(3500, 60000);
        doNothing().when(deliveryPolicyService).updateDeliveryPolicy(any(), any());

        // when & then
        mockMvc.perform(put("/api/orders/delivery-policy")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @DisplayName("배송 정책 수정 실패 - 권한 없음")
    @Test
    void updateDeliveryPolicy_Fail_AccessDenied() throws Exception {
        // given
        DeliveryPolicyUpdateRequest request = new DeliveryPolicyUpdateRequest(3500, 60000);
        doThrow(new com.nhnacademy.order.common.exception.AccessDeniedException("")).when(deliveryPolicyService).updateDeliveryPolicy(any(), any());

        // when & then
        mockMvc.perform(put("/api/orders/delivery-policy")
                        .header("X-USER-ID", "2")
                        .header("X-USER-ROLE", "MEMBER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
