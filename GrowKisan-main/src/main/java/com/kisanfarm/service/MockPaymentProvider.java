package com.kisanfarm.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Development payment provider. "Succeeds" instantly without a real gateway.
 * Enabled when kisanfarm.payment.provider=mock (the default).
 */
@Component
@ConditionalOnProperty(name = "kisanfarm.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public CreateResult createPayment(String orderNumber, BigDecimal amount, String currency) {
        String id = "mock_pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        return new CreateResult(id, "mock_secret_" + id);
    }

    @Override
    public boolean verifyPayment(Confirmation confirmation, String storedProviderPaymentId) {
        // In mock mode, any created payment is considered successful.
        String id = confirmation != null && confirmation.providerPaymentId() != null
                ? confirmation.providerPaymentId() : storedProviderPaymentId;
        return id != null && id.startsWith("mock_pi_");
    }
}
