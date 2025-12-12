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
import com.nhnacademy.order.order.dto.NonMemberOrderCancelRequest;
import com.nhnacademy.order.order.dto.NonMemberOrderGetRequest;
import com.nhnacademy.order.order.dto.OrderCreateRequest;
import com.nhnacademy.order.order.dto.OrderResponse;
import com.nhnacademy.order.orderitem.dto.NonMemberOrderItemStatusPatchRequest;
import com.nhnacademy.order.orderitem.dto.OrderItemStatusPatchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Order API", description = "주문 관련 API")
public interface OrderController {

    @Operation(summary = "모든 주문 조회 (관리자용)", description = "관리자 권한으로 시스템의 모든 주문 내역을 페이지별로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 주문 목록을 조회했습니다."),
            @ApiResponse(responseCode = "403", description = "요청에 필요한 권한이 없습니다.")
    })
    @GetMapping("/orders/admin")
    ResponseEntity<Page<OrderResponse>> getAllOrderByAdmin(
            @Parameter(description = "페이지네이션 정보(예: ?page=0&size=10)", required = true) Pageable pageable,
            UserInfo userInfo);

    @Operation(summary = "내 주문 목록 조회 (회원용)", description = "로그인한 회원의 모든 주문 내역을 페이지별로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 주문 목록을 조회했습니다."),
            @ApiResponse(responseCode = "403", description = "요청에 필요한 권한이 없습니다.")
    })
    @GetMapping("/orders")
    ResponseEntity<Page<OrderResponse>> getAllOrderByCustomer(
            @Parameter(description = "페이지네이션 정보(예: ?page=0&size=10)", required = true) Pageable pageable,
            UserInfo userInfo);

    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다. 회원 또는 비회원으로 주문할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "주문이 성공적으로 생성되었습니다."),
            @ApiResponse(responseCode = "400", description = "요청 데이터가 유효하지 않습니다."),
            @ApiResponse(responseCode = "500", description = "주문 생성 중 오류가 발생했습니다.")
    })
    @PostMapping("/orders")
    ResponseEntity<OrderResponse> createOrder(
            @Parameter(description = "생성할 주문의 상세 정보", required = true) @RequestBody @Valid OrderCreateRequest request,
            UserInfo userInfo);

    @Operation(summary = "주문 상세 조회 (회원용)", description = "특정 주문의 상세 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 주문 정보를 조회했습니다."),
            @ApiResponse(responseCode = "403", description = "해당 주문에 접근할 권한이 없습니다."),
            @ApiResponse(responseCode = "404", description = "해당 주문을 찾을 수 없습니다.")
    })
    @GetMapping("/orders/{orderId}")
    ResponseEntity<OrderResponse> getOrderByCustomer(
            @Parameter(description = "조회할 주문의 ID", required = true) @PathVariable Long orderId,
            UserInfo userInfo);

    @Operation(summary = "주문 상세 조회 (비회원용)", description = "주문 번호와 비밀번호를 사용하여 비회원 주문의 상세 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "성공적으로 주문 정보를 조회했습니다."),
            @ApiResponse(responseCode = "400", description = "요청 데이터가 유효하지 않습니다."),
            @ApiResponse(responseCode = "404", description = "주문 번호 또는 비밀번호가 일치하는 주문을 찾을 수 없습니다.")
    })
    @PostMapping("/orders/non-members/")
    ResponseEntity<OrderResponse> getOrderForNonMember(
            @Parameter(description = "비회원 주문 조회를 위한 정보", required = true) @RequestBody @Valid NonMemberOrderGetRequest request);

    @Operation(summary = "주문 상품 상태 변경 (회원/관리자)", description = "특정 주문에 포함된 개별 상품의 상태를 변경합니다. (예: 구매 확정)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "주문 상품 상태가 성공적으로 변경되었습니다."),
            @ApiResponse(responseCode = "400", description = "요청 데이터가 유효하지 않습니다."),
            @ApiResponse(responseCode = "403", description = "해당 주문에 접근할 권한이 없습니다."),
            @ApiResponse(responseCode = "404", description = "해당 주문 또는 상품을 찾을 수 없습니다.")
    })
    @PatchMapping("/orders/{orderId}/items/{orderItemId}")
    ResponseEntity<OrderResponse> patchOrderItemStatusByCustomer(
            @Parameter(description = "주문의 ID", required = true) @PathVariable Long orderId,
            @Parameter(description = "주문 상품의 ID", required = true) @PathVariable Long orderItemId,
            @Parameter(description = "변경할 상태 정보", required = true) @RequestBody OrderItemStatusPatchRequest request,
            UserInfo userInfo);

    @Operation(summary = "주문 상품 상태 변경 (비회원)", description = "비회원 주문에 포함된 개별 상품의 상태를 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "주문 상품 상태가 성공적으로 변경되었습니다."),
            @ApiResponse(responseCode = "400", description = "요청 데이터가 유효하지 않습니다."),
            @ApiResponse(responseCode = "404", description = "주문 번호 또는 비밀번호가 일치하는 주문을 찾을 수 없거나, 해당 상품을 찾을 수 없습니다.")
    })
    @PatchMapping("/orders/non-members/{orderId}/items/{orderItemId}")
    ResponseEntity<OrderResponse> patchOrderItemStatusForNonMember(
            @Parameter(description = "주문의 ID", required = true) @PathVariable Long orderId,
            @Parameter(description = "주문 상품의 ID", required = true) @PathVariable Long orderItemId,
            @Parameter(description = "변경할 상태 정보 및 비밀번호", required = true) @RequestBody NonMemberOrderItemStatusPatchRequest request);

    @Operation(summary = "주문 취소 (회원/관리자)", description = "특정 주문을 취소합니다. 결제 이전의 주문만 취소할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "주문이 성공적으로 취소되었습니다."),
            @ApiResponse(responseCode = "403", description = "해당 주문을 취소할 권한이 없습니다."),
            @ApiResponse(responseCode = "404", description = "해당 주문을 찾을 수 없습니다."),
            @ApiResponse(responseCode = "409", description = "이미 처리되어 취소할 수 없는 상태의 주문입니다.")
    })
    @DeleteMapping("/orders/{orderId}")
    ResponseEntity<OrderResponse> cancelOrder(
            @Parameter(description = "취소할 주문의 ID", required = true) @PathVariable Long orderId,
            UserInfo userInfo);

    @Operation(summary = "주문 취소 (비회원)", description = "비회원 주문을 비밀번호를 통해 취소합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "주문이 성공적으로 취소되었습니다."),
            @ApiResponse(responseCode = "400", description = "요청 데이터가 유효하지 않습니다."),
            @ApiResponse(responseCode = "404", description = "주문 번호 또는 비밀번호가 일치하는 주문을 찾을 수 없습니다."),
            @ApiResponse(responseCode = "409", description = "이미 처리되어 취소할 수 없는 상태의 주문입니다.")
    })
    @DeleteMapping("/orders/non-members/{orderId}")
    ResponseEntity<OrderResponse> cancelOrderForNonMember(
            @Parameter(description = "취소할 주문의 ID", required = true) @PathVariable Long orderId,
            @Parameter(description = "주문 취소를 위한 비밀번호", required = true) @RequestBody @Valid NonMemberOrderCancelRequest request);

}
