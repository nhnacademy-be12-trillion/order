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

package com.nhnacademy.order.packaging.controller;

import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.packaging.dto.PackagingResponse;
import com.nhnacademy.order.packaging.dto.PackagingUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Packaging API", description = "포장지 관련 API")
public interface PackagingController {
    @Operation(summary = "모든 포장지 조회", description = "모든 포장지 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "포장지 조회 성공"),
    })
    ResponseEntity<List<PackagingResponse>> getAllPackaging();

    @Operation(summary = "포장지 수정", description = "포장지 정보를 수정합니다. 관리자 권한이 필요합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "포장지 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @ApiResponse(responseCode = "403", description = "권한이 없습니다."),
            @ApiResponse(responseCode = "404", description = "포장지를 찾을 수 없습니다.")
    })
    ResponseEntity<Void> updatePackaging(@Parameter(description = "수정할 포장지의 ID", required = true) @PathVariable Long packagingId,
                                         @Parameter(description = "수정할 포장지 정보", required = true) @RequestBody PackagingUpdateRequest request,
                                         UserInfo userInfo);

    @Operation(summary = "포장지 삭제", description = "포장지를 삭제합니다. 관리자 권한이 필요합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "포장지 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한이 없습니다."),
            @ApiResponse(responseCode = "404", description = "포장지를 찾을 수 없습니다.")
    })
    ResponseEntity<Void> removePackaging(@Parameter(description = "삭제할 포장지의 ID", required = true) @PathVariable Long packagingId,
                                         UserInfo userInfo);
}
