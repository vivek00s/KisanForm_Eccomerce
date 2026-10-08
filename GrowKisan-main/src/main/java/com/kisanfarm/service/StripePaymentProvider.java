package com.kisanfarm.service;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Stripe payment provider. Enabled when kisanfarm.payment.provider=stripe.
 * Creates a PaymentIntent server-side (amount computed by the backend) and
 * returns its client secret for the frontend to complete the payment. The
 * secret key stays on the backend. verifyPayment re-checks the intent status
 * with Stripe so a frontend success screen alone is never trusted.
 */
@Component
@ConditionalOnProperty(name = "kisanfarm.payment.provider", havingValue = "stripe")
public class StripePaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentProvider.class);

    @Value("${kisanfarm.payment.secret-key:}")
    private String secretKey;

    @PostConstruct
    void init() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Stripe provider active but STRIPE_SECRET_KEY is not set.");
        }
        Stripe.apiKey = secretKey;
        log.info("Stripe payment provider initialized.");
    }

    @Override
    public String name() {
        return "stripe";
    }

    @Override
    public CreateResult createPayment(String orderNumber, BigDecimal amount, String currency) {
        try {
            // Stripe expects the amount in the smallest currency unit (paise for INR).
            long minor = amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(minor)
                    .setCurrency(currency.toLowerCase())
                    .putMetadata("order_number", orderNumber)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                    .build();
            PaymentIntent intent = PaymentIntent.create(params);
            return new CreateResult(intent.getId(), intent.getClientSecret());
        } catch (Exception e) {
            throw new RuntimeException("Stripe payment creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyPayment(String providerPaymentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(providerPaymentId);
            return "succeeded".equals(intent.getStatus());
        } catch (Exception e) {
            log.warn("Stripe verify failed for {}: {}", providerPaymentId, e.getMessage());
            return false;
        }
    }
}
