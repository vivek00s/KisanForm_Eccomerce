package com.kisanfarm.service;

import java.math.BigDecimal;

/**
 * Payment provider abstraction. Implementations: MockPaymentProvider (dev),
 * StripePaymentProvider (production). Secrets stay on the backend only.
 */
public interface PaymentProvider {

    String name();

    /**
     * Create a payment intent/order for an order.
     * <p>providerPaymentId: the id the provider assigns (Stripe PaymentIntent id,
     * Razorpay order id). clientSecret: a value safe to send to the frontend to
     * complete the payment (Stripe client secret, or the Razorpay public Key ID).
     */
    CreateResult createPayment(String orderNumber, BigDecimal amount, String currency);

    /**
     * Confirm/verify a payment is actually completed on the provider side. The
     * frontend-reported fields are passed through here; the provider decides
     * whether they prove a real, completed payment. A frontend "success" screen
     * is never trusted on its own.
     *
     * @param confirmation values returned by the gateway on the client
     *                     (paymentId / signature). All may be null for providers
     *                     that only need the stored providerPaymentId.
     * @param storedProviderPaymentId the providerPaymentId saved at create time
     */
    boolean verifyPayment(Confirmation confirmation, String storedProviderPaymentId);

    record CreateResult(String providerPaymentId, String clientSecret) {}

    /** Values the client returns after the gateway step. */
    record Confirmation(String providerPaymentId, String razorpaySignature) {}
}
