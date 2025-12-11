package com.nhnacademy.order.order.dto;

import jakarta.validation.constraints.NotBlank;

public record NonMemberOrderCancelRequest(
    @NotBlank
    String nonMemberPassword
) {}
