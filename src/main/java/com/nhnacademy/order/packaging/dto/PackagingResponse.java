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

package com.nhnacademy.order.packaging.dto;

import com.nhnacademy.order.packaging.domain.Packaging;

public record PackagingResponse(
    Long packagingId,
    String packagingType,
    int packagingPrice
) {
    public static PackagingResponse create(Packaging packaging) {
        return new PackagingResponse(
            packaging.getPackagingId(),
            packaging.getPackagingType(),
            packaging.getPackagingPrice()
        );
    }
}
