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

import com.nhnacademy.order.common.dto.UserInfo;
import com.nhnacademy.order.delivery.dto.DeliveryPolicyResponse;
import com.nhnacademy.order.delivery.dto.DeliveryPolicyUpdateRequest;

public interface DeliveryPolicyService {
    DeliveryPolicyResponse getDeliveryPolicy();
    void updateDeliveryPolicy(UserInfo userInfo, DeliveryPolicyUpdateRequest request);
}
