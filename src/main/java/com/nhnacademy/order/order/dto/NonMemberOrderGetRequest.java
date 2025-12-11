package com.nhnacademy.order.order.dto;

import jakarta.validation.constraints.NotBlank;

public record NonMemberOrderGetRequest(
    String orderNumber,

    @NotBlank
    String nonMemberPassword
) {}
