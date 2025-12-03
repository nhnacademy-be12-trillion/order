package com.nhnacademy.order.orderitem.service;

import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.orderitem.repository.OrderItemRepository;
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
@DisplayName("OrderItemRefundService 단위 테스트")
class OrderItemRefundServiceTest {

    @InjectMocks
    private OrderItemRefundService orderItemRefundService;

    @Mock
    private OrderItemRepository orderItemRepository;

    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        testOrderItem = OrderItem.createInitial(
                null,
                1L,
                2,
                LocalDateTime.now().plusDays(3),
                500
        );
        testOrderItem.completeOrderItem(10000);
    }

    @Test
    @DisplayName("주문 항목 환불 완료 성공 - AWAITING_REFUND_FINALIZATION 상태")
    void completeOrderItem_Success_AwaitingRefundFinalization() {
        // given
        testOrderItem.setOrderItemStatus(OrderItemStatus.AWAITING_REFUND_FINALIZATION);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);

        // when
        orderItemRefundService.completeOrderItem(testOrderItem);

        // then
        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository, times(1)).save(captor.capture());
        
        OrderItem savedItem = captor.getValue();
        assertThat(savedItem.getOrderItemStatus()).isEqualTo(OrderItemStatus.RETURNED);
    }

    @Test
    @DisplayName("주문 항목 환불 완료 - DELIVERED 상태에서 호출")
    void completeOrderItem_FromDeliveredStatus() {
        // given
        testOrderItem.setOrderItemStatus(OrderItemStatus.DELIVERED);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);

        // when
        orderItemRefundService.completeOrderItem(testOrderItem);

        // then
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
        assertThat(testOrderItem.getOrderItemStatus()).isEqualTo(OrderItemStatus.RETURNED);
    }

    @Test
    @DisplayName("주문 항목 환불 완료 - PENDING 상태에서 호출")
    void completeOrderItem_FromPendingStatus() {
        // given
        testOrderItem.setOrderItemStatus(OrderItemStatus.PENDING);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);

        // when
        orderItemRefundService.completeOrderItem(testOrderItem);

        // then
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
        assertThat(testOrderItem.getOrderItemStatus()).isEqualTo(OrderItemStatus.RETURNED);
    }

    @Test
    @DisplayName("주문 항목 환불 완료 - CANCELED 상태")
    void completeOrderItem_CanceledStatus() {
        // given
        testOrderItem.setOrderItemStatus(OrderItemStatus.CANCELED);
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);

        // when
        orderItemRefundService.completeOrderItem(testOrderItem);

        // then
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
        assertThat(testOrderItem.getOrderItemStatus()).isEqualTo(OrderItemStatus.RETURNED);
    }

    @Test
    @DisplayName("주문 항목 환불 완료 - 다양한 상태에서의 전환")
    void completeOrderItem_VariousStatusTransitions() {
        // given
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);

        OrderItemStatus[] statuses = {
            OrderItemStatus.PENDING,
            OrderItemStatus.PREPARING,
            OrderItemStatus.SHIPPED,
            OrderItemStatus.DELIVERED
        };

        // when & then
        for (OrderItemStatus status : statuses) {
            testOrderItem.setOrderItemStatus(status);
            orderItemRefundService.completeOrderItem(testOrderItem);
            assertThat(testOrderItem.getOrderItemStatus()).isEqualTo(OrderItemStatus.RETURNED);
        }

        verify(orderItemRepository, times(statuses.length)).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("주문 항목 환불 완료 - 여러 항목 순차 처리")
    void completeOrderItem_MultipleItemsSequential() {
        // given
        OrderItem item1 = OrderItem.createInitial(null, 1L, 1, LocalDateTime.now(), 0);
        OrderItem item2 = OrderItem.createInitial(null, 2L, 2, LocalDateTime.now(), 500);
        OrderItem item3 = OrderItem.createInitial(null, 3L, 3, LocalDateTime.now(), 1000);
        
        item1.setOrderItemStatus(OrderItemStatus.DELIVERED);
        item2.setOrderItemStatus(OrderItemStatus.SHIPPED);
        item3.setOrderItemStatus(OrderItemStatus.PENDING);

        when(orderItemRepository.save(any(OrderItem.class)))
            .thenReturn(item1)
            .thenReturn(item2)
            .thenReturn(item3);

        // when
        orderItemRefundService.completeOrderItem(item1);
        orderItemRefundService.completeOrderItem(item2);
        orderItemRefundService.completeOrderItem(item3);

        // then
        verify(orderItemRepository, times(3)).save(any(OrderItem.class));
        assertThat(item1.getOrderItemStatus()).isEqualTo(OrderItemStatus.RETURNED);
        assertThat(item2.getOrderItemStatus()).isEqualTo(OrderItemStatus.RETURNED);
        assertThat(item3.getOrderItemStatus()).isEqualTo(OrderItemStatus.RETURNED);
    }
}