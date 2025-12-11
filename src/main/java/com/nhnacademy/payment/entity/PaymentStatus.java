package com.nhnacademy.payment.entity;

//이름이 겹침 Payment 전용임.
public enum PaymentStatus {
    DONE, // 결제 승인
    PARTIAL_CANCELED, //결제 부분 취소 상태
    CANCELED //전액 취소


}
