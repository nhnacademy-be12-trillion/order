package com.nhnacademy.payment.dto.response;

import com.nhnacademy.payment.entity.Payment;
import lombok.Builder;

import java.time.LocalDateTime;

//사용자 전용
@Builder
public record PaymentResponse (
        String orderNumber, //주문 번호
        Integer totalAmount, //결제 당시 총 금액
        String status, //결제 상태
        LocalDateTime requestedAt, //요청 날짜
        LocalDateTime approvedAt, //승인 날짜
        String receiptUrl
){
    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .orderNumber(payment.getOrder().getOrderNumber())
                .totalAmount(payment.getOrder().getOrderDetails().totalPrice())
                .status(payment.getPaymentStatus().toString())
                .requestedAt(payment.getPaymentRequestAt())
                .approvedAt(payment.getPaymentApprovedAt())
                .receiptUrl(payment .getPaymentReceipt())
                .build();
    }
}
