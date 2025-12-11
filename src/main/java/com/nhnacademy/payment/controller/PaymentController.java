package com.nhnacademy.payment.controller;

import com.nhnacademy.payment.dto.reqeust.PaymentCancelRequestDto;
import com.nhnacademy.payment.dto.reqeust.PaymentRequestDto;
import com.nhnacademy.payment.dto.response.PaymentResponse;
import com.nhnacademy.payment.entity.Payment;
import com.nhnacademy.payment.service.PaymentService;
import com.nhnacademy.payment.service.impl.PaymentFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentFlowService paymentFlowService;


    // 사용자 결제 승인
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody PaymentRequestDto request) {

        Payment payment = paymentFlowService.confirmPayment(request);

        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    // 사용자 orderNumber로 조회
    @GetMapping("/{orderNumber}")
    public ResponseEntity<?> getPayment(@PathVariable String orderNumber) {
        Payment payment = paymentService.getPaymentByOrderNumber(orderNumber);
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }


    //그럼 여기는? 취소 요청만? 배송 전의 상태만
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelPayment(@RequestBody PaymentCancelRequestDto request) {
        paymentFlowService.cancelPayment(
                request.orderNumber(),
                request.cancelReason(),
                request.cancelAmount() // null이면 서비스가 알아서 전액 취소로 처리함
        );

        return ResponseEntity.ok("결제 취소 요청이 정상적으로 처리되었습니다.");
    }


    //사용자의 결제 전체 내역을 보게 해줌
    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> getPayments(Pageable pageable) {
        Page<Payment> payments = paymentService.getAllPayments(pageable);
        Page<PaymentResponse> responses = payments.map(PaymentResponse::from);
        return ResponseEntity.ok(responses);
    }

 }
