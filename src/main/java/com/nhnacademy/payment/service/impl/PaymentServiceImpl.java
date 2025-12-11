package com.nhnacademy.payment.service.impl;


import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.order.order.repository.OrderRepository;
import com.nhnacademy.payment.dto.response.TossPaymentResponseDto;
import com.nhnacademy.payment.entity.Payment;
import com.nhnacademy.payment.entity.PaymentStatus;
import com.nhnacademy.payment.exception.PaymentNotFoundException;
import com.nhnacademy.payment.exception.PaymentStateConflictException;
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
    public Payment savePayment(TossPaymentResponseDto response, Order order) {

        Payment savePayment = Payment.builder()
                .paymentKey(response.getPaymentKey())
                .paymentStatus(PaymentStatus.DONE)
                .paymentRequestAt(LocalDateTime.parse(response.getRequestedAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .paymentApprovedAt(LocalDateTime.parse(response.getApprovedAt(), DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .paymentReceipt(response.getReceipt().getUrl())
                .order(order)
                .totalAmount(response.getTotalAmount()) //결제 승인당시 최종 금액 -> 불변
                .build();

        paymentRepository.save(savePayment);
        savePayment.getOrder().setOrderStatus(com.nhnacademy.order.order.domain.OrderStatus.COMPLETED);
        orderRepository.save(order);


        return savePayment;
    }

    //결제 취소시
    @Override
    @Transactional
    public void updatePaymentCanceledStatus(Payment payment, Integer cancelAmount) {

        //해당 결제를 찾을 수 없다면?
        Payment findPayment = paymentRepository.findById((payment.getPaymentId())).orElseThrow(
                () -> new PaymentNotFoundException("결제 정보가 존재하지 않습니다."));

        //이미 취소된 결제 건 이라면?
        if(findPayment.getPaymentStatus().equals(PaymentStatus.CANCELED)) {
            throw new PaymentStateConflictException("이미 전체 취소된 결제 건 입니다.");
        }

        findPayment.cancelPayment(cancelAmount);

        if(findPayment.getPaymentStatus() == PaymentStatus.CANCELED) {
            findPayment.getOrder().setOrderStatus(com.nhnacademy.order.order.domain.OrderStatus.CANCELED);
        }
    }
    //결제 정보 반환 -> 서버에서 처리할때만 사용할듯
    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByOrderNumber(String orderNumber) {
        Payment payment = paymentRepository.findByOrder_OrderNumber(orderNumber);
        if(payment==null){
            throw new PaymentNotFoundException("결제 정보가 존재하지 않습니다.");
        }

        return payment;
    }


    //특정 결제 내역 조회 -> 사용자에게 보여줄 페이지(단건 조회)
    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {

        return paymentRepository.findById(paymentId).orElseThrow(
                () -> new PaymentNotFoundException("결제 정보가 존재하지 않습니다."+paymentId)
        );
    }

    //결제 내역 전체 조회,
    @Override
    @Transactional(readOnly = true)
    public Page<Payment> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable);
    }
}
