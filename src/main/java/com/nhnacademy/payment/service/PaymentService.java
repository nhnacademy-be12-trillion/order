package com.nhnacademy.payment.service;

import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.payment.domain.Payment;
import com.nhnacademy.payment.dto.reqeust.PaymentRequestDto;
import com.nhnacademy.payment.dto.response.PaymentResponse;
import com.nhnacademy.payment.dto.response.TossPaymentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    PaymentResponse savePayment(TossPaymentResponseDto response, Order order);

    void updatePaymentCanceledStatus(Payment payment);

    Payment getPaymentByOrderNumber(String orderNumber);

    PaymentResponse getPaymentById(Long paymentId);

    Page<PaymentResponse> getAllPayments(Pageable pageable);

}
