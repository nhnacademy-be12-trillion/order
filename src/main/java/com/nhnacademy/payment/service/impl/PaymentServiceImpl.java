package com.nhnacademy.payment.service.impl;


import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.payment.domain.Payment;
import com.nhnacademy.payment.domain.PaymentStatus;
import com.nhnacademy.payment.dto.response.PaymentResponse;
import com.nhnacademy.payment.dto.response.TossPaymentResponseDto;
import com.nhnacademy.payment.exception.PaymentAlreadyCanceledException;
import com.nhnacademy.payment.exception.PaymentNotFoundException;
import com.nhnacademy.payment.repository.PaymentRepository;
import com.nhnacademy.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    //DB에 담는 시점에서만 Transactional 호출하면 됨.
    @Override
    @Transactional
    public PaymentResponse savePayment(TossPaymentResponseDto response, Order order) {

        Payment savePayment = Payment.builder()
                .paymentKey(response.getPaymentKey())
                .paymentStatus(PaymentStatus.DONE)
                .paymentRequestAt(LocalDateTime.parse(response.getRequestedAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .paymentApprovedAt(LocalDateTime.parse(response.getApprovedAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .paymentReceipt(response.getReceipt().getUrl())
                .order(order)
                .build();

        paymentRepository.save(savePayment);
        savePayment.getOrder().setPaymentStatus(com.nhnacademy.order.order.domain.PaymentStatus.COMPLETED);
        orderRepository.save(order);


        return PaymentResponse.from(savePayment);
    }

    //결제 취소시
    @Override
    @Transactional
    public void updatePaymentCanceledStatus(Payment payment) {

        Payment findPayment = paymentRepository.findById((payment.getPaymentId())).orElseThrow(
                () -> new PaymentNotFoundException(payment.getPaymentKey()));

        if(findPayment.getPaymentStatus().equals(PaymentStatus.CANCELED)) {
            throw new PaymentAlreadyCanceledException("이미 결제 취소된 상품입니다.: "  +payment.getPaymentKey());
        }

        findPayment.cancelPayment();
        findPayment.getOrder().setPaymentStatus(com.nhnacademy.order.order.domain.PaymentStatus.CANCELED);
    }

    //결제 정보 반환 -> 서버에서 처리할때만 사용할듯
    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByOrderNumber(String orderNumber) {
        Payment payment =  paymentRepository.findByOrder_OrderNumber(orderNumber);
        if(payment==null){
            throw new PaymentNotFoundException("결제 정보가 존재하지 않습니다.");
        }
        return payment;
    }

    //특정 결제 내역 조회 -> 사용자에게 보여줄 페이지(단건 조회)
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(
                () -> new PaymentNotFoundException("결제 정보가 존재하지 않습니다.")
        );

        return PaymentResponse.from(payment);
    }


    //결제 내역 전체 조회,
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(PaymentResponse::from);
    }
}
