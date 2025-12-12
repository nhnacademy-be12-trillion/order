package com.nhnacademy.order.scheduler;

import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.ordersaga.creation.domain.OrderCreateSaga;
import com.nhnacademy.order.ordersaga.creation.service.OrderCreateOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @InjectMocks
    private ReconciliationService reconciliationService;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderCreateOrchestrator orderCreateOrchestrator;
    
    // Add other mocks as needed for other tests...

    @Test
    @DisplayName("processStuckCreateSagaCompensation: 멈춘 주문 생성 사가에 대한 보상 처리 호출")
    void testProcessStuckCreateSagaCompensation() {
        // given
        long orderId = 1L;
        OrderCreateSaga saga = mock(OrderCreateSaga.class);
        when(saga.getOrderId()).thenReturn(orderId);

        Order order = mock(Order.class);
        when(orderRepository.findOrderWithItemsByOrderId(orderId)).thenReturn(Optional.of(order));

        // when
        reconciliationService.processStuckCreateSagaCompensation(saga);

        // then
        verify(orderRepository, times(1)).findOrderWithItemsByOrderId(orderId);
        verify(orderCreateOrchestrator, times(1)).compensate(saga, order);
    }
    
    @Test
    @DisplayName("processStuckCreateSagaCompensation: 주문을 찾지 못하면 보상 처리 안함")
    void testProcessStuckCreateSagaCompensation_OrderNotFound() {
        // given
        long orderId = 2L;
        OrderCreateSaga saga = mock(OrderCreateSaga.class);
        when(saga.getOrderId()).thenReturn(orderId);

        when(orderRepository.findOrderWithItemsByOrderId(orderId)).thenReturn(Optional.empty());

        // when
        reconciliationService.processStuckCreateSagaCompensation(saga);

        // then
        verify(orderRepository, times(1)).findOrderWithItemsByOrderId(orderId);
        verify(orderCreateOrchestrator, never()).compensate(any(), any());
    }
}
