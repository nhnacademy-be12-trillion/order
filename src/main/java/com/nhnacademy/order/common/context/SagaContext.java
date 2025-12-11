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

package com.nhnacademy.order.common.context;

import java.util.UUID;

public class SagaContext {
    private static final ThreadLocal<UUID> sagaIdHolder = new ThreadLocal<>();

    public static void set(UUID sagaId) {
        sagaIdHolder.set(sagaId);
    }

    public static UUID get() {
        return sagaIdHolder.get();
    }

    public static void clear() {
        sagaIdHolder.remove();
    }
}
