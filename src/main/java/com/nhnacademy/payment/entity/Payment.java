package com.nhnacademy.payment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nhnacademy.order.order.domain.Order;
import com.nhnacademy.payment.exception.PaymentValidationException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "payment_key")
    private String paymentKey;

    @Column(name = "payment_status")
    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_requested_at")
    private LocalDateTime paymentRequestAt;//결제 요청일

    @Column(name = "payment_approved_at")
    private LocalDateTime paymentApprovedAt; // 결제 승인일

    @Column(name = "payment_receipt")
    private String paymentReceipt;

    @Column(name = "total_amount") // 결제 당시 최종 가격 -> 얘는 불변
    private Integer totalAmount;

    @Column(name =  "balance_amount") //취소 가능 잔액일듯 --> 부분 취소 마다 줄어듦
    private Integer balanceAmount;

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Builder
    public Payment(String paymentKey, PaymentStatus paymentStatus, LocalDateTime paymentRequestAt,
                   LocalDateTime paymentApprovedAt, String paymentReceipt, Order order, Integer totalAmount, Integer balanceAmount) {
        this.paymentKey = paymentKey;
        this.paymentStatus = paymentStatus;
        this.paymentRequestAt = paymentRequestAt;
        this.paymentApprovedAt = paymentApprovedAt;
        this.paymentReceipt = paymentReceipt;
        this.order = order;
        this.totalAmount = totalAmount;
        this.balanceAmount = totalAmount;
    }

    //결제 취소 메서드
    public void cancelPayment(Integer cancelAmount) {
        //취소 가능한 금액보다 취소하려는 금액이 더 크다면?
        if(this.balanceAmount < cancelAmount){
            throw new PaymentValidationException("취소 가능한 잔액이 부족합니다.");
        }
        //취소 된 경우에는 취소 가능한 금액에서 취소 금액을 차감
        this.balanceAmount -= cancelAmount;

        //취소 가능한 금액이 0원 이라면 => 전체 취소 처리, 남아있다면 -> 부분취소 처리임.
        if(this.balanceAmount == 0){
            this.paymentStatus = PaymentStatus.CANCELED;
        }else{
            this.paymentStatus = PaymentStatus.PARTIAL_CANCELED;
        }
    }
}
