package com.nhnacademy.order.packaging.service;

import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.packaging.domain.Packaging;
import com.nhnacademy.order.packaging.dto.PackagingResponse;
import com.nhnacademy.order.packaging.dto.PackagingUpdateRequest;
import com.nhnacademy.order.packaging.exception.PackagingNotFoundException;
import com.nhnacademy.order.packaging.repository.PackagingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PackagingServiceImplTest {

    @Mock
    private PackagingRepository packagingRepository;

    @InjectMocks
    private PackagingServiceImpl packagingService;

    private Packaging packaging1;
    private Packaging packaging2;

    @BeforeEach
    void setUp() {
        packaging1 = new Packaging(1L, "선물포장", 1000);
        packaging2 = new Packaging(2L, "일반포장", 0);
    }

    @DisplayName("모든 포장지 조회 성공")
    @Test
    void getAllPackaging_Success() {
        // given
        given(packagingRepository.findAll()).willReturn(List.of(packaging1, packaging2));

        // when
        List<PackagingResponse> responses = packagingService.getAllPackaging();

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).packagingType()).isEqualTo("선물포장");
        assertThat(responses.get(1).packagingType()).isEqualTo("일반포장");
    }

    @DisplayName("포장지 가격 수정 성공")
    @Test
    void updatePackaging_Success() {
        // given
        UserInfo userInfo = new UserInfo(1L, "ADMIN");
        Long packagingId = 1L;
        PackagingUpdateRequest request = new PackagingUpdateRequest(1500);
        given(packagingRepository.findById(packagingId)).willReturn(Optional.of(packaging1));

        // when
        packagingService.updatePackaging(userInfo, packagingId, request);

        // then
        verify(packagingRepository).save(any(Packaging.class));
        assertThat(packaging1.getPackagingPrice()).isEqualTo(1500);
    }

    @DisplayName("포장지 가격 수정 실패 - 존재하지 않는 포장지")
    @Test
    void updatePackaging_Fail_PackagingNotFound() {
        // given
        UserInfo userInfo = new UserInfo(1L, "ADMIN");
        Long packagingId = 3L;
        PackagingUpdateRequest request = new PackagingUpdateRequest(1500);
        given(packagingRepository.findById(packagingId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> packagingService.updatePackaging(userInfo, packagingId, request))
                .isInstanceOf(PackagingNotFoundException.class)
                .hasMessage("존재하지 않는 포장 정책: " + packagingId);
    }

    @DisplayName("포장지 삭제 성공")
    @Test
    void deletePackaging_Success() {
        // given
        UserInfo userInfo = new UserInfo(1L, "ADMIN");
        Long packagingId = 1L;
        given(packagingRepository.existsById(packagingId)).willReturn(true);

        // when
        packagingService.deletePackaging(userInfo, packagingId);

        // then
        verify(packagingRepository).deleteById(packagingId);
    }

    @DisplayName("포장지 삭제 실패 - 존재하지 않는 포장지")
    @Test
    void deletePackaging_Fail_PackagingNotFound() {
        // given
        UserInfo userInfo = new UserInfo(1L, "ADMIN");
        Long packagingId = 3L;
        given(packagingRepository.existsById(packagingId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> packagingService.deletePackaging(userInfo, packagingId))
                .isInstanceOf(PackagingNotFoundException.class)
                .hasMessage("존재하지 않는 포장 정책: " + packagingId);
    }
}
