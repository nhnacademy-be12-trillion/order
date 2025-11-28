package com.nhnacademy.order.order.service;

import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.dto.OrderCreateRequest;
import com.nhnacademy.order.order.dto.OrderResponse;
import com.nhnacademy.order.orderitem.dto.NonMemberOrderItemStatusPatchRequest;
import com.nhnacademy.order.orderitem.dto.OrderItemStatusPatchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    Page<OrderResponse> findAllOrders(Pageable pageable);
    OrderResponse createOrder(Long memberId, OrderCreateRequest request);
    OrderResponse findOrderByOrderId(Long orderId);
    Page<OrderResponse> findAllOrderByMemberId(Pageable pageable, Long memberId);
    void patchOrderItemStatus(Long memberId, Long orderId, Long orderItemId, OrderItemStatusPatchRequest request);
    void patchOrderItemStatusForNonMember(Long orderId, Long orderItemId, NonMemberOrderItemStatusPatchRequest request);
    OrderResponse findOrderByOrderNumber(String orderNumber, String nonMemberPassword);
}
