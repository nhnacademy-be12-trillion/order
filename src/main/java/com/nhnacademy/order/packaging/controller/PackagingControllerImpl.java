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
import com.nhnacademy.order.packaging.service.PackagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/orders/packaging")
public class PackagingControllerImpl implements PackagingController {
    private final PackagingService packagingService;

    @Override
    @GetMapping
    public ResponseEntity<List<PackagingResponse>> getAllPackaging() {
        List<PackagingResponse> response = packagingService.getAllPackaging();

        return ResponseEntity.ok(response);
    }

    @Override
    @PutMapping("/{packagingId}")
    public ResponseEntity<Void> updatePackaging(@PathVariable Long packagingId,
                                                @RequestBody PackagingUpdateRequest request,
                                                UserInfo userInfo) {

        packagingService.updatePackaging(userInfo, packagingId, request);

        return ResponseEntity.ok().build();
    }

    @Override
    @DeleteMapping("/{packagingId}")
    public ResponseEntity<Void> removePackaging(@PathVariable Long packagingId,
                                                UserInfo userInfo) {

        packagingService.deletePackaging(userInfo, packagingId);

        return ResponseEntity.noContent().build();
    }
}
