/*
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 * + Copyright 2025. NHN Academy Corp. All rights reserved.
 * + * While every precaution has been taken in the preparation of this resource,  assumes no
 * + responsibility for errors or omissions, or for damages resulting from the use of the information
 * + contained herein
 * + No part of this resource may be reproduced, stored in a retrieval system, or transmitted, in any
 * + form or by any means, electronic, mechanical, photocopying, recording, or otherwise, without the
 * + prior written permission.
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 */

package com.nhnacademy.order.order.controller;

import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.order.dto.NonMemberGetRequest;
import com.nhnacademy.order.order.dto.OrderCreateRequest;
import com.nhnacademy.order.order.dto.OrderResponse;
import com.nhnacademy.order.order.service.OrderService;
import com.nhnacademy.order.orderitem.dto.NonMemberOrderItemStatusPatchRequest;
import com.nhnacademy.order.orderitem.dto.OrderItemStatusPatchRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class OrderController {
    private static final String ADMIN_ACCESS_ONLY = "관리자만 이용 가능한 기능";
    private static final String MEMBER_ACCESS_ONLY = "회원만 이용 가능한 기능";

    private final OrderService orderService;

    // 주문 전체 조회 (관리자)
    @GetMapping("/orders/admin")
    public ResponseEntity<Page<OrderResponse>> getAllOrderByAdmin(Pageable pageable,
                                                                  UserInfo userInfo) {
        if (userInfo == null || userInfo.role() == null || !userInfo.role().equals("ADMIN")) {
            throw new AccessDeniedException(ADMIN_ACCESS_ONLY);
        }

        Page<OrderResponse> response = orderService.findAllOrders(pageable);

        return ResponseEntity.ok(response);
    }

    // 주문 전체 조회 (회원)
    @GetMapping("/orders")
    public ResponseEntity<Page<OrderResponse>> getAllOrderByCustomer(Pageable pageable,
                                                                     UserInfo userInfo) {
        if (userInfo == null) {
            throw new AccessDeniedException(MEMBER_ACCESS_ONLY);
        }

        Page<OrderResponse> response = orderService.findAllOrderByMemberId(pageable, userInfo.userId());

        return ResponseEntity.ok(response);
    }

    // 주문 생성
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(@RequestBody @Valid OrderCreateRequest request,
                                                     UserInfo userInfo) {
        // 비회원인 경우 userId가 null
        Long userId = (userInfo == null) ? null : userInfo.userId();

        Long createdOrderId = orderService.createOrder(userId, request);

        OrderResponse createdOrderResponse = orderService.findOrderByOrderId(createdOrderId);

        URI locationUri = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{orderId}")
                .buildAndExpand(createdOrderId)
                .toUri();

        return ResponseEntity.created(locationUri).body(createdOrderResponse);
        //return ResponseEntity.created(locationUri).build();
    }

    // 주문 단건 조회 (회원)
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOrderByCustomer(@PathVariable Long orderId,
                                                            UserInfo userInfo) {
        if (userInfo == null) {
            throw new AccessDeniedException(MEMBER_ACCESS_ONLY);
        }

        OrderResponse response = orderService.findOrderByCustomer(userInfo.userId(), orderId);

        return ResponseEntity.ok(response);
    }

    // 주문 단건 조회 (비회원)
    @PostMapping("/orders/non-members/")
    public ResponseEntity<OrderResponse> getOrderForNonMember(@RequestBody @Valid NonMemberGetRequest request) {
        OrderResponse response = orderService.findOrderByOrderNumber(request.orderNumber(), request.nonMemberPassword());

        return ResponseEntity.ok(response);
    }

    // 주문 상품 상태 변경 (회원, 관리자)
    @PatchMapping("/orders/{orderId}/items/{orderItemId}")
    public ResponseEntity<OrderResponse> patchOrderItemStatusByCustomer(@PathVariable Long orderId, @PathVariable Long orderItemId,
                                                                        @RequestBody OrderItemStatusPatchRequest request,
                                                                        UserInfo userInfo) {
        if (userInfo == null) {
            throw new AccessDeniedException(MEMBER_ACCESS_ONLY);
        }

        orderService.patchOrderItemStatus(userInfo.userId(), orderId, orderItemId, request);

        return ResponseEntity.ok().build();
    }

    // 주문 상품 상태 변경 (비회원)
    @PatchMapping("/orders/non-members/{orderId}/items/{orderItemId}")
    public ResponseEntity<OrderResponse> patchOrderItemStatusForNonMember(@PathVariable Long orderId, @PathVariable Long orderItemId,
                                                                          @RequestBody NonMemberOrderItemStatusPatchRequest request) {

        orderService.patchOrderItemStatusForNonMember(orderId, orderItemId, request);

        return ResponseEntity.ok().build();
    }
}
