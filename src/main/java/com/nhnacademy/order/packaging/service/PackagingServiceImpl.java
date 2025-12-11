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

package com.nhnacademy.order.packaging.service;

import com.nhnacademy.order.common.aop.AuthRole;
import com.nhnacademy.order.common.aop.CheckAuth;
import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.packaging.domain.Packaging;
import com.nhnacademy.order.packaging.dto.PackagingResponse;
import com.nhnacademy.order.packaging.dto.PackagingUpdateRequest;
import com.nhnacademy.order.packaging.exception.PackagingNotFoundException;
import com.nhnacademy.order.packaging.repository.PackagingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PackagingServiceImpl implements PackagingService {
    private final PackagingRepository packagingRepository;

    private static final String PACKAGING_NOT_FOUND_MESSAGE = "존재하지 않는 포장 정책: ";

    @Override
    @Transactional(readOnly = true)
    public List<PackagingResponse> getAllPackaging() {
        List<Packaging> packagingList = packagingRepository.findAll();

        return packagingList.stream()
                .map(PackagingResponse::create)
                .toList();
    }

    @Override
    @Transactional
    @CheckAuth(role = AuthRole.ADMIN)
    public void updatePackaging(UserInfo userInfo, Long packagingId, PackagingUpdateRequest request) {
        Packaging packaging = packagingRepository.findById(packagingId)
                .orElseThrow(() -> new PackagingNotFoundException(PACKAGING_NOT_FOUND_MESSAGE + packagingId));

        packaging.updatePrice(request.packagingPrice());

        packagingRepository.save(packaging);
    }

    @Override
    @Transactional
    @CheckAuth(role = AuthRole.ADMIN)
    public void deletePackaging(UserInfo userInfo, Long packagingId) {
        if (!packagingRepository.existsById(packagingId)) {
            throw new PackagingNotFoundException(PACKAGING_NOT_FOUND_MESSAGE + packagingId);
        }

        packagingRepository.deleteById(packagingId);
    }
}
