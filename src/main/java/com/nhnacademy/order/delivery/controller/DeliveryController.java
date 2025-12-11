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

package com.nhnacademy.order.delivery.controller;

import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.delivery.dto.DeliveryPolicyResponse;
import com.nhnacademy.order.delivery.dto.DeliveryPolicyUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Delivery API", description = "배송 정책 관련 API")
public interface DeliveryController {
    @Operation(summary = "배송 정책 조회", description = "현재 적용된 배송 정책을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "배송 정책 조회 성공"),
            @ApiResponse(responseCode = "404", description = "배송 정책을 찾을 수 없습니다.")
    })
    ResponseEntity<DeliveryPolicyResponse> getDeliveryPolicy();

    @Operation(summary = "배송 정책 수정", description = "배송 정책을 수정합니다. 관리자 권한이 필요합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "배송 정책 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @ApiResponse(responseCode = "403", description = "권한이 없습니다.")
    })
    ResponseEntity<Void> updateDeliveryPolicy(@Parameter(description = "수정할 배송 정책 정보", required = true) @RequestBody DeliveryPolicyUpdateRequest request,
                                              UserInfo userInfo);
}
