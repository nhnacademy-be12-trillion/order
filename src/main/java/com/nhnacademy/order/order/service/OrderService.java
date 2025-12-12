package com.nhnacademy.order.order.service;

import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.order.dto.OrderCreateRequest;
import com.nhnacademy.order.order.dto.OrderResponse;
import com.nhnacademy.order.orderitem.dto.NonMemberOrderItemStatusPatchRequest;
import com.nhnacademy.order.orderitem.dto.OrderItemStatusPatchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    Page<OrderResponse> findAllOrders(UserInfo userInfo, Pageable pageable);
    OrderResponse createOrder(UserInfo userInfo, OrderCreateRequest request);
    OrderResponse findOrderByOrderId(UserInfo userInfo, Long orderId);
    Page<OrderResponse> findAllOrderByMemberId(UserInfo userInfo, Pageable pageable);
    OrderResponse patchOrderItemStatus(UserInfo userInfo, Long orderId, Long orderItemId, OrderItemStatusPatchRequest request);
    OrderResponse patchOrderItemStatusForNonMember(Long orderId, Long orderItemId, NonMemberOrderItemStatusPatchRequest request);
    OrderResponse findOrderByOrderNumber(String orderNumber, String nonMemberPassword);
    void cancelOrder(UserInfo userInfo, Long orderId);
    void cancelOrderForNonMember(Long orderId, String nonMemberPassword);
}
