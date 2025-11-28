package com.nhnacademy.payment.dto.response;

import com.nhnacademy.payment.domain.Payment;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PaymentResponse (
        Long paymentId,
        String paymentKey,
        String orderNumber,
        Integer totalAmount,
        String status,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        String receiptUrl
){
    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentKey(payment.getPaymentKey())
                .orderNumber(payment.getOrder().getOrderNumber())
                .totalAmount(payment.getOrder().getOrderDetails().totalPrice())
                .status(payment.getPaymentStatus().toString())
                .requestedAt(payment.getPaymentRequestAt())
                .approvedAt(payment.getPaymentApprovedAt())
                .receiptUrl(payment.getPaymentReceipt())
                .build();
    }
}
