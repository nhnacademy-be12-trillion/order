package com.nhnacademy.order.packaging.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.order.packaging.dto.PackagingResponse;
import com.nhnacademy.order.packaging.dto.PackagingUpdateRequest;
import com.nhnacademy.order.packaging.exception.PackagingNotFoundException;
import com.nhnacademy.order.packaging.service.PackagingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "toss.secret-key=test-key")
@AutoConfigureMockMvc
class PackagingControllerImplTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PackagingService packagingService;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("모든 포장지 조회 성공")
    @Test
    void getAllPackaging_Success() throws Exception {
        // given
        List<PackagingResponse> responses = List.of(
                new PackagingResponse(1L, "선물포장", 1000),
                new PackagingResponse(2L, "일반포장", 0)
        );
        given(packagingService.getAllPackaging()).willReturn(responses);

        // when & then
        mockMvc.perform(get("/api/orders/packaging"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].packagingType").value("선물포장"))
                .andExpect(jsonPath("$[1].packagingType").value("일반포장"));
    }

    @DisplayName("포장지 수정 성공")
    @Test
    void updatePackaging_Success() throws Exception {
        // given
        PackagingUpdateRequest request = new PackagingUpdateRequest(1500);
        doNothing().when(packagingService).updatePackaging(any(), anyLong(), any());

        // when & then
        mockMvc.perform(put("/api/orders/packaging/1")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @DisplayName("포장지 수정 실패 - 존재하지 않는 포장지")
    @Test
    void updatePackaging_Fail_PackagingNotFound() throws Exception {
        // given
        PackagingUpdateRequest request = new PackagingUpdateRequest(1500);
        doThrow(new PackagingNotFoundException("존재하지 않는 포장 정책: 1")).when(packagingService).updatePackaging(any(), anyLong(), any());

        // when & then
        mockMvc.perform(put("/api/orders/packaging/1")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @DisplayName("포장지 삭제 성공")
    @Test
    void deletePackaging_Success() throws Exception {
        // given
        doNothing().when(packagingService).deletePackaging(any(), anyLong());

        // when & then
        mockMvc.perform(delete("/api/orders/packaging/1")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "ADMIN"))
                .andExpect(status().isNoContent());
    }

    @DisplayName("포장지 삭제 실패 - 존재하지 않는 포장지")
    @Test
    void deletePackaging_Fail_PackagingNotFound() throws Exception {
        // given
        doThrow(new PackagingNotFoundException("존재하지 않는 포장 정책: 1")).when(packagingService).deletePackaging(any(), anyLong());

        // when & then
        mockMvc.perform(delete("/api/orders/packaging/1")
                        .header("X-USER-ID", "1")
                        .header("X-USER-ROLE", "ADMIN"))
                .andExpect(status().isNotFound());
    }
}
