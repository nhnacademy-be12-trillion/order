package com.nhnacademy.payment.service;

import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.payment.dto.response.TossPaymentResponseDto;
import com.nhnacademy.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    Payment savePayment(TossPaymentResponseDto response, Order order);

    void updatePaymentCanceledStatus(Payment payment, Integer cancelAmount);

    Payment getPaymentByOrderNumber(String orderNumber);

    Payment getPaymentById(Long paymentId);

    Page<Payment> getAllPayments(Pageable pageable);

}
