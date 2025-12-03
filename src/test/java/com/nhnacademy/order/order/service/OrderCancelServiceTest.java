package com.nhnacademy.order.order.service;

import com.nhnacademy.order.order.domain.*;
import com.nhnacademy.order.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCancelService 단위 테스트")
class OrderCancelServiceTest {

    @InjectMocks
    private OrderCancelService orderCancelService;

    @Mock
    private OrderRepository orderRepository;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        OrdererInfo ordererInfo = new OrdererInfo("홍길동", "010-1234-5678");
        ReceiverInfo receiverInfo = new ReceiverInfo("이순신", "010-9876-5432", "서울시");
        OrderDetails orderDetails = OrderDetails.createInitial(
                "12345",
                LocalDateTime.now().plusDays(3),
                1000,
                1L
        );
        
        testOrder = Order.createInitial(
                1L,
                null,
                ordererInfo,
                receiverInfo,
                orderDetails
        );
    }

    @Test
    @DisplayName("주문 취소 완료 성공 - AWAITING_CANCELLATION 상태")
    void completeOrder_Success_AwaitingCancellation() {
        // given
        testOrder.setOrderStatus(OrderStatus.AWAITING_CANCELLATION);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // when
        orderCancelService.completeOrder(testOrder);

        // then
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(captor.capture());
        
        Order savedOrder = captor.getValue();
        assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("주문 취소 완료 - 이미 취소된 주문은 재처리하지 않음")
    void completeOrder_AlreadyCanceled_NoReprocessing() {
        // given
        testOrder.setOrderStatus(OrderStatus.CANCELED);

        // when
        orderCancelService.completeOrder(testOrder);

        // then
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 취소 완료 - PENDING 상태에서 호출")
    void completeOrder_PendingStatus() {
        // given
        testOrder.setOrderStatus(OrderStatus.PENDING);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // when
        orderCancelService.completeOrder(testOrder);

        // then
        verify(orderRepository, times(1)).save(any(Order.class));
        assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("주문 취소 완료 - COMPLETED 상태에서 호출")
    void completeOrder_CompletedStatus() {
        // given
        testOrder.setOrderStatus(OrderStatus.COMPLETED);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // when
        orderCancelService.completeOrder(testOrder);

        // then
        verify(orderRepository, times(1)).save(any(Order.class));
        assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("주문 취소 완료 - 여러 번 호출해도 한 번만 저장")
    void completeOrder_MultipleCallsIdempotent() {
        // given
        testOrder.setOrderStatus(OrderStatus.PENDING);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // when
        orderCancelService.completeOrder(testOrder);
        testOrder.setOrderStatus(OrderStatus.AWAITING_CANCELLATION);
        orderCancelService.completeOrder(testOrder);

        // then
        // 첫 호출에서만 저장, 두 번째는 AWAITING_CANCELLATION이므로 return
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 취소 완료 - CREATING 상태")
    void completeOrder_CreatingStatus() {
        // given
        testOrder.setOrderStatus(OrderStatus.CREATING);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // when
        orderCancelService.completeOrder(testOrder);

        // then
        verify(orderRepository, times(1)).save(any(Order.class));
        assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("주문 취소 완료 - CREATION_FAILED 상태")
    void completeOrder_CreationFailedStatus() {
        // given
        testOrder.setOrderStatus(OrderStatus.CREATION_FAILED);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // when
        orderCancelService.completeOrder(testOrder);

        // then
        verify(orderRepository, times(1)).save(any(Order.class));
        assertThat(testOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    @DisplayName("주문 취소 완료 - null 주문 처리")
    void completeOrder_NullOrder() {
        // when & then
        assertThatThrownBy(() -> orderCancelService.completeOrder(null))
                .isInstanceOf(NullPointerException.class);
        
        verify(orderRepository, never()).save(any(Order.class));
    }
}