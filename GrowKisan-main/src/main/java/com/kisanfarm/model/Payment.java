package com.kisanfarm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Payment record, kept separate from order status. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private Long id;
    private Long orderId;
    private String provider;             // mock | stripe
    private String providerPaymentId;    // Stripe PaymentIntent id
    private String providerOrderId;
    private String status;               // PENDING|SUCCESS|FAILED|REFUNDED
    private BigDecimal amount;
    private String currency;
    private String clientSecret;         // Stripe client secret for the frontend
    private String failureReason;
}
