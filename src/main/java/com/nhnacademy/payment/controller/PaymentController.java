package com.nhnacademy.payment.controller;

import com.nhnacademy.payment.domain.Payment;
import com.nhnacademy.payment.domain.PaymentStatus;
import com.nhnacademy.payment.dto.reqeust.PaymentRequestDto;
import com.nhnacademy.payment.dto.response.PaymentResponse;
import com.nhnacademy.payment.service.PaymentService;
import com.nhnacademy.payment.service.impl.PaymentFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentFlowService paymentFlowService;


    @PostMapping("/success")
    public ResponseEntity<?> createPaymentSuccess(@RequestBody PaymentRequestDto request) {

        PaymentResponse payment = paymentFlowService.confirmPayment(request);

        return ResponseEntity.ok(payment);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPaymentById(@PathVariable("paymentId") Long paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelPayment(@RequestBody Map<String, String> requestBody) {
        String orderNumber= requestBody.get("orderNumber");
        String cancelReason = requestBody.get("cancelReason");

        paymentFlowService.cancelPayment(orderNumber, cancelReason);

        return ResponseEntity.ok("결제 취소 완료");
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> getPayments(Pageable pageable) {
        return ResponseEntity.ok(paymentService.getAllPayments(pageable));
    }

 }
