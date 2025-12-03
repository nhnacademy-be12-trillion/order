package com.nhnacademy.order.scheduler;

import com.nhnacademy.order.order.domain.*;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.order.service.OrderCancelService;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.orderitem.repository.OrderItemRepository;
import com.nhnacademy.order.orderitem.service.OrderItemRefundService;
import com.nhnacademy.order.ordersaga.cancellation.domain.OrderCancelSaga;
import com.nhnacademy.order.ordersaga.cancellation.repository.OrderCancelSagaRepository;
import com.nhnacademy.order.ordersaga.creation.domain.OrderCreateSaga;
import com.nhnacademy.order.ordersaga.creation.repository.OrderCreateSagaRepository;
import com.nhnacademy.order.ordersaga.creation.service.OrderCreateOrchestrator;
import com.nhnacademy.order.ordersaga.itemrefund.domain.OrderItemRefundSaga;
import com.nhnacademy.order.ordersaga.itemrefund.repository.OrderItemRefundSagaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconciliationService 단위 테스트")
class ReconciliationServiceTest {

    @InjectMocks
    private ReconciliationService reconciliationService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderCreateSagaRepository orderCreateSagaRepository;

    @Mock
    private OrderCancelSagaRepository orderCancelSagaRepository;

    @Mock
    private OrderItemRefundSagaRepository orderItemRefundSagaRepository;

    @Mock
    private OrderCancelService orderCancelService;

    @Mock
    private OrderCreateOrchestrator orderCreateOrchestrator;

    @Mock
    private OrderItemRefundService orderItemRefundService;

    private Order testOrder;
    private OrderItem testOrderItem;
    private OrderCreateSaga testCreateSaga;
    private OrderCancelSaga testCancelSaga;

    @BeforeEach
    void setUp() {
        OrdererInfo ordererInfo = new OrdererInfo("홍길동", "010-1234-5678");
        ReceiverInfo receiverInfo = new ReceiverInfo("이순신", "010-9876-5432", "서울시");
        OrderDetails orderDetails = OrderDetails.createInitial(
                "12345",
                LocalDateTime.now().plusDays(3),
                0,
                null
        );
        
        testOrder = Order.createInitial(1L, null, ordererInfo, receiverInfo, orderDetails);
        testOrderItem = OrderItem.createInitial(testOrder, 1L, 2, LocalDateTime.now().plusDays(3), 500);
        testCreateSaga = OrderCreateSaga.create(1L);
        testCancelSaga = OrderCancelSaga.create(1L);
    }

    @Test
    @DisplayName("멈춘 주문 생성 보상 처리 성공")
    void compensateStuckCreationOrder_Success() {
        // given
        testOrder.setOrderStatus(OrderStatus.AWAITING_POST_PROCESSING);
        when(orderCreateSagaRepository.findByOrderId(anyLong())).thenReturn(Optional.of(testCreateSaga));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(orderCreateOrchestrator).compensate(any(), any());

        // when
        reconciliationService.compensateStuckCreationOrder(testOrder);

        // then
        verify(orderCreateOrchestrator, times(1)).compensate(testCreateSaga, testOrder);
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    @DisplayName("멈춘 주문 생성 보상 처리 - 사가 없음")
    void compensateStuckCreationOrder_SagaNotFound() {
        // given
        testOrder.setOrderStatus(OrderStatus.AWAITING_POST_PROCESSING);
        when(orderCreateSagaRepository.findByOrderId(anyLong())).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // when
        reconciliationService.compensateStuckCreationOrder(testOrder);

        // then
        verify(orderCreateOrchestrator, never()).compensate(any(), any());
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    @DisplayName("멈춘 주문 취소 처리 성공")
    void processStuckCancellationOrder_Success() {
        // given
        testOrder.setOrderStatus(OrderStatus.AWAITING_CANCELLATION);
        doNothing().when(orderCancelService).completeOrder(any(Order.class));

        // when
        reconciliationService.processStuckCancellationOrder(testOrder);

        // then
        verify(orderCancelService, times(1)).completeOrder(testOrder);
    }

    @Test
    @DisplayName("멈춘 주문 항목 환불 처리 성공")
    void processStuckRefundOrderItem_Success() {
        // given
        testOrderItem.setOrderItemStatus(OrderItemStatus.AWAITING_REFUND_FINALIZATION);
        doNothing().when(orderItemRefundService).completeOrderItem(any(OrderItem.class));

        // when
        reconciliationService.processStuckRefundOrderItem(testOrderItem);

        // then
        verify(orderItemRefundService, times(1)).completeOrderItem(testOrderItem);
    }

    @Test
    @DisplayName("멈춘 생성 사가 보상 처리 성공")
    void processStuckCreateSagaCompensation_Success() {
        // given
        when(orderRepository.findOrderWithItemsByOrderId(anyLong())).thenReturn(Optional.of(testOrder));
        doNothing().when(orderCreateOrchestrator).compensate(any(), any());

        // when
        reconciliationService.processStuckCreateSagaCompensation(testCreateSaga);

        // then
        verify(orderCreateOrchestrator, times(1)).compensate(testCreateSaga, testOrder);
    }

    @Test
    @DisplayName("완료된 취소 사가 브릿징 성공")
    void processCompletedCancelSagaBridge_Success() {
        // given
        testOrder.setOrderStatus(OrderStatus.PENDING);
        when(orderRepository.findOrderWithItemsByOrderId(anyLong())).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderCancelSagaRepository.save(any(OrderCancelSaga.class))).thenReturn(testCancelSaga);

        // when
        reconciliationService.processCompletedCancelSagaBridge(testCancelSaga);

        // then
        verify(orderRepository, times(1)).save(testOrder);
        verify(orderCancelSagaRepository, times(1)).save(testCancelSaga);
    }

    @Test
    @DisplayName("완료된 환불 사가 브릿징 성공")
    void processCompletedRefundSagaBridge_Success() {
        // given
        testOrderItem.setOrderItemStatus(OrderItemStatus.DELIVERED);
        OrderItemRefundSaga refundSaga = OrderItemRefundSaga.create(1L, 1L);
        
        when(orderItemRepository.findById(anyLong())).thenReturn(Optional.of(testOrderItem));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);
        when(orderItemRefundSagaRepository.save(any(OrderItemRefundSaga.class))).thenReturn(refundSaga);

        // when
        reconciliationService.processCompletedRefundSagaBridge(refundSaga);

        // then
        verify(orderItemRepository, times(1)).save(testOrderItem);
        verify(orderItemRefundSagaRepository, times(1)).save(refundSaga);
    }

    @Test
    @DisplayName("보상 처리 실패 시 로그만 기록하고 계속 진행")
    void compensateStuckCreationOrder_ExceptionHandling() {
        // given
        testOrder.setOrderStatus(OrderStatus.AWAITING_POST_PROCESSING);
        when(orderCreateSagaRepository.findByOrderId(anyLong()))
                .thenThrow(new RuntimeException("Database error"));

        // when
        reconciliationService.compensateStuckCreationOrder(testOrder);

        // then
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("멈춘 생성 사가 보상 처리 - 주문 없음")
    void processStuckCreateSagaCompensation_OrderNotFound() {
        // given
        when(orderRepository.findOrderWithItemsByOrderId(anyLong())).thenReturn(Optional.empty());

        // when
        reconciliationService.processStuckCreateSagaCompensation(testCreateSaga);

        // then
        verify(orderCreateOrchestrator, never()).compensate(any(), any());
    }

    @Test
    @DisplayName("완료된 취소 사가 브릿징 - 주문이 이미 취소 상태")
    void processCompletedCancelSagaBridge_AlreadyCanceled() {
        // given
        testOrder.setOrderStatus(OrderStatus.CANCELED);
        when(orderRepository.findOrderWithItemsByOrderId(anyLong())).thenReturn(Optional.of(testOrder));

        // when
        reconciliationService.processCompletedCancelSagaBridge(testCancelSaga);

        // then
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderCancelSagaRepository, never()).save(any(OrderCancelSaga.class));
    }

    @Test
    @DisplayName("완료된 환불 사가 브릿징 - 주문 항목이 이미 RETURNED 상태")
    void processCompletedRefundSagaBridge_AlreadyReturned() {
        // given
        testOrderItem.setOrderItemStatus(OrderItemStatus.RETURNED);
        OrderItemRefundSaga refundSaga = OrderItemRefundSaga.create(1L, 1L);
        
        when(orderItemRepository.findById(anyLong())).thenReturn(Optional.of(testOrderItem));

        // when
        reconciliationService.processCompletedRefundSagaBridge(refundSaga);

        // then
        verify(orderItemRepository, never()).save(any(OrderItem.class));
        verify(orderItemRefundSagaRepository, never()).save(any(OrderItemRefundSaga.class));
    }
}