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

package com.nhnacademy.order.delivery.service;

import com.nhnacademy.order.common.aop.AuthRole;
import com.nhnacademy.order.common.aop.CheckAuth;
import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.delivery.domain.DeliveryPolicy;
import com.nhnacademy.order.delivery.dto.DeliveryPolicyResponse;
import com.nhnacademy.order.delivery.dto.DeliveryPolicyUpdateRequest;
import com.nhnacademy.order.delivery.exception.PolicyNotConfiguredException;
import com.nhnacademy.order.delivery.repository.DeliveryPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class DeliveryPolicyServiceImpl implements DeliveryPolicyService {
    private final DeliveryPolicyRepository deliveryPolicyRepository;

    private DeliveryPolicy getPolicy() {
        return deliveryPolicyRepository.findFirstByOrderByDeliveryPolicyIdAsc()
                .orElseThrow(() -> new PolicyNotConfiguredException("배송 정책이 설정되지 않음"));
    }

    @Transactional(readOnly = true)
    public DeliveryPolicyResponse getDeliveryPolicy() {
        DeliveryPolicy deliveryPolicy = getPolicy();

        return DeliveryPolicyResponse.create(deliveryPolicy);
    }

    @Transactional
    @CheckAuth(role = AuthRole.ADMIN)
    public void updateDeliveryPolicy(UserInfo userInfo, DeliveryPolicyUpdateRequest request) {
        DeliveryPolicy deliveryPolicy = getPolicy();

        deliveryPolicy.update(request.deliveryPolicyFee(), request.deliveryPolicyThreshold());

        deliveryPolicyRepository.save(deliveryPolicy);
    }
}
