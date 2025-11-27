package com.nhnacademy.payment.service.impl;

import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.domain.PaymentStatus;
import com.nhnacademy.order.order.exception.OrderNotFoundException;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.payment.config.TossPaymentClient;
import com.nhnacademy.payment.domain.Payment;
import com.nhnacademy.payment.dto.reqeust.PaymentRequestDto;
import com.nhnacademy.payment.dto.response.PaymentResponse;
import com.nhnacademy.payment.dto.response.TossPaymentResponseDto;
import com.nhnacademy.payment.exception.PaymentAlreadyApprovedException;
import com.nhnacademy.payment.exception.PaymentAlreadyCanceledException;
import com.nhnacademy.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentFlowService {
    private final PaymentService paymentService;
    private final TossPaymentClient tossPaymentClient;
    private final OrderRepository orderRepository;

    public PaymentResponse confirmPayment(PaymentRequestDto request) {
        Order order = orderRepository.findByOrderNumber(request.orderNumber())
                .orElseThrow(() -> new OrderNotFoundException(request.orderNumber()));

        if(order.getPaymentStatus().equals(PaymentStatus.COMPLETED)){
            throw new  PaymentAlreadyApprovedException(request.orderNumber());
        }

        TossPaymentResponseDto response;
        try{
            response = tossPaymentClient.confirm(
                    request.paymentKey(),
                    request.orderNumber(),
                    request.amount()
            );
        }catch (Exception e){
            log.error("결제 승인 API 호출 도중 오류 발생 : {}" , e.getMessage());
            throw e;
        }
        try{
            return paymentService.savePayment(response, order);
        }catch (Exception e){
            log.error("결제 정보 저장 중 오류 발생 :{} ", request.orderNumber());
            tossPaymentClient.cancel(request.paymentKey(), request.orderNumber());
            throw new RuntimeException("결제 승인 실패");
        }
    }


    //결제 취소
    public void cancelPayment(String orderNumber,String cancelReason) {
        Payment payment = paymentService.getPaymentByOrderNumber(orderNumber);

        if(payment.getOrder().getPaymentStatus().equals(PaymentStatus.CANCELED)){
            throw new PaymentAlreadyCanceledException("이미 결제 취소된 주문 건 입니다." +payment.getPaymentKey());
        }

        TossPaymentResponseDto response = tossPaymentClient.cancel(payment.getPaymentKey(), cancelReason);

        // 3. DB 상태 업데이트
        if ("CANCELED".equals(response.getStatus())) {
            paymentService.updatePaymentCanceledStatus(payment);
        }
    }
}
