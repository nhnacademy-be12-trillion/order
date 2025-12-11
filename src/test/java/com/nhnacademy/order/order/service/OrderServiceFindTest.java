package com.nhnacademy.order.order.service;

import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.order.domain.OrderStatus;
import com.nhnacademy.order.order.domain.OrdererInfo;
import com.nhnacademy.order.order.domain.ReceiverInfo;
import com.nhnacademy.order.order.dto.NonMemberOrderBaseResponse;
import com.nhnacademy.order.order.dto.OrderBaseResponse;
import com.nhnacademy.order.order.dto.OrderResponse;
import com.nhnacademy.order.order.exception.OrderNotFoundException;
import com.nhnacademy.order.order.exception.OrderPasswordMismatchException;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.order.orderitem.domain.OrderItemStatus;
import com.nhnacademy.order.orderitem.dto.OrderItemResponse;
import com.nhnacademy.order.orderitem.repository.OrderItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceFindTest {

    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("주문 ID로 단건 조회 - 성공")
    void findOrderByOrderId_Success() {
        // given
        long orderId = 1L;
        UserInfo userInfo = new UserInfo(1L, "MEMBER");
        OrderBaseResponse dummyBaseResponse = new OrderBaseResponse(
                orderId, 1L, "ORD-1234", LocalDateTime.now(), OrderStatus.PENDING,
                40500, 34500, 0, new OrdererInfo("홍길동", "010-1234-5678"),
                new ReceiverInfo("이순신", "010-9876-5432", "서울")
        );
        List<OrderItemResponse> dummyItems = List.of(
                new OrderItemResponse(1L, orderId, 1L, "테스트 책 이름", 2, 20000, 500, OrderItemStatus.SHIPPED)
        );

        when(orderRepository.findBaseOrderById(orderId)).thenReturn(Optional.of(dummyBaseResponse));
        when(orderItemRepository.findOrderItemByOrder_OrderId(orderId)).thenReturn(dummyItems);

        // when
        OrderResponse response = orderServiceImpl.findOrderByOrderId(userInfo, orderId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(orderId);
        verify(orderRepository, times(1)).findBaseOrderById(orderId);
        verify(orderItemRepository, times(1)).findOrderItemByOrder_OrderId(orderId);
    }

    @Test
    @DisplayName("주문 ID로 단건 조회 - 실패: 존재하지 않는 주문")
    void findOrderByOrderId_Failure_NotFound() {
        // given
        long nonExistentOrderId = 999L;
        UserInfo userInfo = new UserInfo(1L, "MEMBER");
        when(orderRepository.findBaseOrderById(nonExistentOrderId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(OrderNotFoundException.class, () -> orderServiceImpl.findOrderByOrderId(userInfo, nonExistentOrderId));
        verify(orderRepository, times(1)).findBaseOrderById(nonExistentOrderId);
        verify(orderItemRepository, never()).findOrderItemByOrder_OrderId(anyLong());
    }

    @Test
    @DisplayName("비회원 주문 조회 - 성공")
    void findOrderByOrderNumber_Success() {
        // given
        String orderNumber = "ORD-NON-MEMBER-123";
        String rawPassword = "password1234";
        String encodedPassword = "encoded-password";
        NonMemberOrderBaseResponse dummyResponse = new NonMemberOrderBaseResponse(
                2L, encodedPassword, null, orderNumber, LocalDateTime.now(),
                OrderStatus.PENDING, 30000, 0, new OrdererInfo("비회원", "010-0000-0000"),
                new ReceiverInfo("받는사람", "010-1111-2222", "주소")
        );
        when(orderRepository.findNonMemberOrderByOrderNumber(orderNumber)).thenReturn(Optional.of(dummyResponse));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(orderItemRepository.findOrderItemByOrder_OrderId(2L)).thenReturn(Collections.emptyList());

        // when
        OrderResponse response = orderServiceImpl.findOrderByOrderNumber(orderNumber, rawPassword);

        // then
        assertThat(response).isNotNull();
        assertThat(response.orderNumber()).isEqualTo(orderNumber);
    }

    @Test
    @DisplayName("비회원 주문 조회 - 실패: 비밀번호 불일치")
    void findOrderByOrderNumber_Failure_PasswordMismatch() {
        // given
        String orderNumber = "ORD-NON-MEMBER-123";
        String wrongPassword = "wrong-password";
        String encodedPassword = "encoded-password";
        NonMemberOrderBaseResponse dummyResponse = new NonMemberOrderBaseResponse(
                2L, encodedPassword, null, orderNumber, LocalDateTime.now(),
                OrderStatus.PENDING, 30000, 0, new OrdererInfo("비회원", "010-0000-0000"),
                new ReceiverInfo("받는사람", "010-1111-2222", "주소")
        );
        when(orderRepository.findNonMemberOrderByOrderNumber(orderNumber)).thenReturn(Optional.of(dummyResponse));
        when(passwordEncoder.matches(wrongPassword, encodedPassword)).thenReturn(false);

        // when & then
        assertThrows(OrderPasswordMismatchException.class, () -> {
            orderServiceImpl.findOrderByOrderNumber(orderNumber, wrongPassword);
        });
    }

    @Test
    @DisplayName("회원 주문 목록 조회 - 성공")
    void findAllOrderByMemberId_Success() {
        // given
        UserInfo userInfo = new UserInfo(1L, "MEMBER");
        Pageable pageable = Pageable.ofSize(10);
        List<OrderBaseResponse> baseResponses = List.of(
                new OrderBaseResponse(1L, userInfo.userId(), "ORD-1", LocalDateTime.now(), OrderStatus.COMPLETED, 100, 100, 0, null, null),
                new OrderBaseResponse(2L, userInfo.userId(), "ORD-2", LocalDateTime.now(), OrderStatus.COMPLETED, 200, 200, 0, null, null)
        );
        Page<OrderBaseResponse> pagedBaseResponse = new PageImpl<>(baseResponses, pageable, baseResponses.size());
        when(orderRepository.findAllBaseOrderByMemberId(pageable, userInfo.userId())).thenReturn(pagedBaseResponse);
        when(orderItemRepository.findAllByOrderIds(anyList())).thenReturn(Collections.emptyList());

        // when
        Page<OrderResponse> resultPage = orderServiceImpl.findAllOrderByMemberId(userInfo, pageable);

        // then
        assertThat(resultPage).isNotNull();
        assertThat(resultPage.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("회원 주문 목록 조회 - 성공: 주문 내역 없음")
    void findAllOrderByMemberId_Success_NoOrders() {
        // given
        UserInfo userInfo = new UserInfo(2L, "MEMBER");
        Pageable pageable = Pageable.ofSize(10);
        when(orderRepository.findAllBaseOrderByMemberId(pageable, userInfo.userId())).thenReturn(Page.empty(pageable));

        // when
        Page<OrderResponse> resultPage = orderServiceImpl.findAllOrderByMemberId(userInfo, pageable);

        // then
        assertThat(resultPage).isNotNull();
        assertThat(resultPage.isEmpty()).isTrue();
    }
}

