package com.nhnacademy.order.order.service;

import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.exception.OrderPasswordMismatchException;
import com.nhnacademy.order.order.exception.OrderStatusTransitionException;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.orderitem.domain.OrderItem;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.orderitem.dto.NonMemberOrderItemStatusPatchRequest;
import com.nhnacademy.order.orderitem.dto.OrderItemStatusPatchRequest;
import com.nhnacademy.order.orderitem.exception.OrderItemNotFoundException;
import com.nhnacademy.order.orderitem.repository.OrderItemRepository;
import com.nhnacademy.order.ordersaga.cancellation.service.OrderCancelOrchestrator;
import com.nhnacademy.order.ordersaga.itemrefund.service.NonMemberOrderItemRefundOrchestrator;
import com.nhnacademy.order.ordersaga.itemrefund.service.OrderItemRefundOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceUpdateTest {

    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OrderCancelOrchestrator orderCancelOrchestrator;
    @Mock
    private OrderItemRefundOrchestrator orderItemRefundOrchestrator;
    @Mock
    private NonMemberOrderItemRefundOrchestrator nonMemberOrderItemRefundOrchestrator;

    @Test
    @DisplayName("회원 주문 상품 상태 변경 - 성공: 환불 요청 시 오케스트레이터 호출")
    void patchOrderItemStatus_Success_MemberReturn() {
        // given
        long orderId = 1L;
        long orderItemId = 101L;
        UserInfo adminInfo = new UserInfo(999L, "ADMIN");

        OrderItem orderItem = mock(OrderItem.class);
        Order order = mock(Order.class);

        when(orderRepository.findOrderWithItemsByOrderId(orderId)).thenReturn(Optional.of(order));
        when(order.findOrderItemInOrder(orderItemId)).thenReturn(orderItem);

        OrderItemStatusPatchRequest request = new OrderItemStatusPatchRequest(OrderItemStatus.RETURNED);

        // when
        orderServiceImpl.patchOrderItemStatus(adminInfo, orderId, orderItemId, request);

        // then
        // 환불 상태 변경 요청 시, 회원 환불 오케스트레이터가 올바른 인자들로 호출되는지 검증
        verify(orderItemRefundOrchestrator, times(1)).processItemRefund(adminInfo.userId(), order, orderItem);
        // 비회원 환불 오케스트레이터는 호출되지 않아야 함
        verify(nonMemberOrderItemRefundOrchestrator, never()).processNonMemberItemRefund(any(), any());
    }

    @Test
    @DisplayName("비회원 주문 상품 상태 변경 - 성공: 환불 요청")
    void patchOrderItemStatusForNonMember_Success_ReturnRequest() {
        // given
        long orderId = 2L;
        long orderItemId = 201L;
        String rawPassword = "password1234";
        String encodedPassword = "encoded-password";

        OrderItem orderItem = mock(OrderItem.class);
        when(orderItem.getOrderItemId()).thenReturn(orderItemId);

        Order nonMemberOrder = mock(Order.class);
        when(nonMemberOrder.getNonMemberPassword()).thenReturn(encodedPassword);
        lenient().when(nonMemberOrder.findOrderItemInOrder(orderItemId)).thenReturn(orderItem);
        when(nonMemberOrder.getOrderItems()).thenReturn(List.of(orderItem));
        when(orderItem.getOrderItemStatus()).thenReturn(OrderItemStatus.DELIVERED);
        when(orderItem.getShippingDate()).thenReturn(LocalDateTime.now());

        when(orderRepository.findOrderWithItemsByOrderId(orderId)).thenReturn(Optional.of(nonMemberOrder));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        NonMemberOrderItemStatusPatchRequest request = new NonMemberOrderItemStatusPatchRequest(rawPassword, OrderItemStatus.RETURN_REQUESTED_CHANGE_OF_MIND);

        // when
        orderServiceImpl.patchOrderItemStatusForNonMember(orderId, orderItemId, request);

        // then
        verify(passwordEncoder, times(1)).matches(rawPassword, encodedPassword);
    }


    @Test
    @DisplayName("주문 전체 취소 - 성공")
    void cancelOrder_Success() {
        // given
        long orderId = 1L;
        UserInfo userInfo = new UserInfo(1L, "MEMBER");

        OrderItem item1 = mock(OrderItem.class);
        when(item1.getOrderItemStatus()).thenReturn(OrderItemStatus.PREPARING);
        Order cancelableOrder = mock(Order.class);
        when(cancelableOrder.getOrderItems()).thenReturn(List.of(item1));

        when(orderRepository.findOrderWithItemsByOrderId(orderId)).thenReturn(Optional.of(cancelableOrder));
        doNothing().when(orderCancelOrchestrator).processCancelOrder(anyLong(), any(Order.class));

        // when
        orderServiceImpl.cancelOrder(userInfo, orderId);

        // then
        verify(orderRepository, times(1)).findOrderWithItemsByOrderId(orderId);
        // ✨ 변경된 로직 검증: OrderServiceImpl이 OrderCancelOrchestrator를 호출하는지 확인
        verify(orderCancelOrchestrator, times(1)).processCancelOrder(userInfo.userId(), cancelableOrder);
    }

    @Test
    @DisplayName("주문 전체 취소 - 실패: 취소 불가능 상태")
    void cancelOrder_Failure_CannotCancel() {
        // given
        long orderId = 1L;
        UserInfo userInfo = new UserInfo(1L, "MEMBER");

        OrderItem item1 = mock(OrderItem.class);
        when(item1.getOrderItemStatus()).thenReturn(OrderItemStatus.SHIPPED); // 취소 불가능 상태
        Order uncancelableOrder = mock(Order.class);
        when(uncancelableOrder.getOrderItems()).thenReturn(List.of(item1));

        when(orderRepository.findOrderWithItemsByOrderId(orderId)).thenReturn(Optional.of(uncancelableOrder));

        // when & then
        assertThrows(OrderStatusTransitionException.class, () -> {
            orderServiceImpl.cancelOrder(userInfo, orderId);
        });

        // 예외가 발생했으므로, 취소 오케스트레이터는 호출되지 않아야 함
        verify(orderCancelOrchestrator, never()).processCancelOrder(any(), any());
    }
}