package com.kisanfarm.service;

import java.math.BigDecimal;

/**
 * Payment provider abstraction. Implementations: MockPaymentProvider (dev),
 * StripePaymentProvider (production). Secrets stay on the backend only.
 */
public interface PaymentProvider {

    String name();

    /** Create a payment intent for an order. */
    CreateResult createPayment(String orderNumber, BigDecimal amount, String currency);

    /** Confirm/verify a payment is actually completed on the provider side. */
    boolean verifyPayment(String providerPaymentId);

    record CreateResult(String providerPaymentId, String clientSecret) {}
}
