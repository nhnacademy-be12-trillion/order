package com.nhnacademy.order.ordersaga.service;

import com.nhnacademy.order.ordersaga.cancellation.domain.CancelSagaStep;
import com.nhnacademy.order.ordersaga.cancellation.domain.OrderCancelSaga;
import com.nhnacademy.order.ordersaga.cancellation.repository.OrderCancelSagaRepository;
import com.nhnacademy.order.ordersaga.creation.domain.CreateSagaStep;
import com.nhnacademy.order.ordersaga.creation.domain.OrderCreateSaga;
import com.nhnacademy.order.ordersaga.creation.repository.OrderCreateSagaRepository;
import com.nhnacademy.order.ordersaga.domain.SagaStatus;
import com.nhnacademy.order.ordersaga.itemrefund.domain.ItemRefundSagaStep;
import com.nhnacademy.order.ordersaga.itemrefund.domain.OrderItemRefundSaga;
import com.nhnacademy.order.ordersaga.itemrefund.repository.OrderItemRefundSagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SagaUpdateService 단위 테스트")
class SagaUpdateServiceTest {

    @InjectMocks
    private SagaUpdateService sagaUpdateService;

    @Mock
    private OrderCreateSagaRepository orderCreateSagaRepository;

    @Mock
    private OrderCancelSagaRepository orderCancelSagaRepository;

    @Mock
    private OrderItemRefundSagaRepository orderItemRefundSagaRepository;

    private OrderCreateSaga testCreateSaga;
    private OrderCancelSaga testCancelSaga;
    private OrderItemRefundSaga testRefundSaga;

    @BeforeEach
    void setUp() {
        testCreateSaga = OrderCreateSaga.create(1L);
        testCancelSaga = OrderCancelSaga.create(1L);
        testRefundSaga = OrderItemRefundSaga.create(1L, 1L);
    }

    @Test
    @DisplayName("주문 생성 사가 단계 업데이트 성공")
    void updateCreateSagaStep_Success() {
        // given
        when(orderCreateSagaRepository.save(any(OrderCreateSaga.class))).thenReturn(testCreateSaga);

        // when
        sagaUpdateService.updateCreateSagaStep(testCreateSaga, CreateSagaStep.STOCK_DECREASED);

        // then
        ArgumentCaptor<OrderCreateSaga> captor = ArgumentCaptor.forClass(OrderCreateSaga.class);
        verify(orderCreateSagaRepository).save(captor.capture());
        
        OrderCreateSaga savedSaga = captor.getValue();
        assertThat(savedSaga.getLastCompletedStep()).isEqualTo(CreateSagaStep.STOCK_DECREASED);
        assertThat(savedSaga.getOverallStatus()).isEqualTo(SagaStatus.PROGRESS);
    }

    @Test
    @DisplayName("주문 생성 사가 상태 업데이트 - COMPLETED")
    void updateCreateSagaStatus_Completed() {
        // given
        when(orderCreateSagaRepository.save(any(OrderCreateSaga.class))).thenReturn(testCreateSaga);

        // when
        sagaUpdateService.updateCreateSagaStatus(testCreateSaga, SagaStatus.COMPLETED);

        // then
        ArgumentCaptor<OrderCreateSaga> captor = ArgumentCaptor.forClass(OrderCreateSaga.class);
        verify(orderCreateSagaRepository).save(captor.capture());
        assertThat(captor.getValue().getOverallStatus()).isEqualTo(SagaStatus.COMPLETED);
    }

    @Test
    @DisplayName("주문 생성 사가 상태 업데이트 - COMPENSATED")
    void updateCreateSagaStatus_Compensated() {
        // given
        when(orderCreateSagaRepository.save(any(OrderCreateSaga.class))).thenReturn(testCreateSaga);

        // when
        sagaUpdateService.updateCreateSagaStatus(testCreateSaga, SagaStatus.COMPENSATED);

        // then
        ArgumentCaptor<OrderCreateSaga> captor = ArgumentCaptor.forClass(OrderCreateSaga.class);
        verify(orderCreateSagaRepository).save(captor.capture());
        assertThat(captor.getValue().getOverallStatus()).isEqualTo(SagaStatus.COMPENSATED);
    }

    @Test
    @DisplayName("주문 취소 사가 단계 업데이트 성공")
    void updateCancelSagaStep_Success() {
        // given
        when(orderCancelSagaRepository.save(any(OrderCancelSaga.class))).thenReturn(testCancelSaga);

        // when
        sagaUpdateService.updateCancelSagaStep(testCancelSaga, CancelSagaStep.PAYMENT_CANCELED);

        // then
        ArgumentCaptor<OrderCancelSaga> captor = ArgumentCaptor.forClass(OrderCancelSaga.class);
        verify(orderCancelSagaRepository).save(captor.capture());
        
        OrderCancelSaga savedSaga = captor.getValue();
        assertThat(savedSaga.getLastCompletedStep()).isEqualTo(CancelSagaStep.PAYMENT_CANCELED);
    }

    @Test
    @DisplayName("주문 취소 사가 상태 업데이트 성공")
    void updateCancelSagaStatus_Success() {
        // given
        when(orderCancelSagaRepository.save(any(OrderCancelSaga.class))).thenReturn(testCancelSaga);

        // when
        sagaUpdateService.updateCancelSagaStatus(testCancelSaga, SagaStatus.COMPLETED);

        // then
        ArgumentCaptor<OrderCancelSaga> captor = ArgumentCaptor.forClass(OrderCancelSaga.class);
        verify(orderCancelSagaRepository).save(captor.capture());
        assertThat(captor.getValue().getOverallStatus()).isEqualTo(SagaStatus.COMPLETED);
    }

    @Test
    @DisplayName("주문 항목 환불 사가 단계 업데이트 성공")
    void updateRefundSagaStep_Success() {
        // given
        when(orderItemRefundSagaRepository.save(any(OrderItemRefundSaga.class))).thenReturn(testRefundSaga);

        // when
        sagaUpdateService.updateItemRefundSagaStep(testRefundSaga, ItemRefundSagaStep.PAYMENT_REFUNDED);

        // then
        ArgumentCaptor<OrderItemRefundSaga> captor = ArgumentCaptor.forClass(OrderItemRefundSaga.class);
        verify(orderItemRefundSagaRepository).save(captor.capture());
        assertThat(captor.getValue().getLastCompletedStep()).isEqualTo(ItemRefundSagaStep.PAYMENT_REFUNDED);
    }

    @Test
    @DisplayName("주문 항목 환불 사가 상태 업데이트 성공")
    void updateRefundSagaStatus_Success() {
        // given
        when(orderItemRefundSagaRepository.save(any(OrderItemRefundSaga.class))).thenReturn(testRefundSaga);

        // when
        sagaUpdateService.updateItemRefundSagaStatus(testRefundSaga, SagaStatus.FAILED);

        // then
        ArgumentCaptor<OrderItemRefundSaga> captor = ArgumentCaptor.forClass(OrderItemRefundSaga.class);
        verify(orderItemRefundSagaRepository).save(captor.capture());
        assertThat(captor.getValue().getOverallStatus()).isEqualTo(SagaStatus.FAILED);
    }

    @Test
    @DisplayName("생성 사가 단계 순차 업데이트")
    void updateCreateSagaStep_Sequential() {
        // given
        when(orderCreateSagaRepository.save(any(OrderCreateSaga.class))).thenReturn(testCreateSaga);

        // when
        sagaUpdateService.updateCreateSagaStep(testCreateSaga, CreateSagaStep.STARTED);
        sagaUpdateService.updateCreateSagaStep(testCreateSaga, CreateSagaStep.STOCK_DECREASED);
        sagaUpdateService.updateCreateSagaStep(testCreateSaga, CreateSagaStep.COUPON_APPLIED);
        sagaUpdateService.updateCreateSagaStep(testCreateSaga, CreateSagaStep.POINT_USED);

        // then
        verify(orderCreateSagaRepository, times(4)).save(any(OrderCreateSaga.class));
        assertThat(testCreateSaga.getLastCompletedStep()).isEqualTo(CreateSagaStep.POINT_USED);
    }

    @Test
    @DisplayName("취소 사가 단계 순차 업데이트")
    void updateCancelSagaStep_Sequential() {
        // given
        when(orderCancelSagaRepository.save(any(OrderCancelSaga.class))).thenReturn(testCancelSaga);

        // when
        sagaUpdateService.updateCancelSagaStep(testCancelSaga, CancelSagaStep.STARTED);
        sagaUpdateService.updateCancelSagaStep(testCancelSaga, CancelSagaStep.PAYMENT_CANCELED);
        sagaUpdateService.updateCancelSagaStep(testCancelSaga, CancelSagaStep.POINT_REFUNDED);

        // then
        verify(orderCancelSagaRepository, times(3)).save(any(OrderCancelSaga.class));
        assertThat(testCancelSaga.getLastCompletedStep()).isEqualTo(CancelSagaStep.POINT_REFUNDED);
    }
}