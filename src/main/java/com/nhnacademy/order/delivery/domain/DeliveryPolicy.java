package com.nhnacademy.order.delivery.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class DeliveryPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deliverypolicy_id")
    private Long deliveryPolicyId;

    @Column(name = "deliverypolicy_fee")
    private int deliveryPolicyFee;

    @Column(name = "deliverypolicy_threshold")
    private int deliveryPolicyThreshold;

    private DeliveryPolicy(int deliveryPolicyFee, int  deliveryPolicyThreshold) {
        this.deliveryPolicyFee = deliveryPolicyFee;
        this.deliveryPolicyThreshold = deliveryPolicyThreshold;
    }

    public static DeliveryPolicy create(int deliveryPolicyFee, int deliveryPolicyThreshold) {
        return new DeliveryPolicy(
            deliveryPolicyFee,
            deliveryPolicyThreshold
        );
    }

    public void update(int deliveryPolicyFee, int deliveryPolicyThreshold) {
        this.deliveryPolicyFee = deliveryPolicyFee;
        this.deliveryPolicyThreshold = deliveryPolicyThreshold;
    }
}
