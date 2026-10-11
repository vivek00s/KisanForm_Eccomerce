package com.kisanfarm.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Razorpay payment provider. Enabled when kisanfarm.payment.provider=razorpay.
 *
 * <p>Flow (web Checkout):
 * <ol>
 *   <li>Backend creates a Razorpay Order for the backend-computed amount and
 *       returns its id. The public Key ID is handed to the frontend as the
 *       "client secret" (it is NOT a secret — only the Key ID is ever exposed).</li>
 *   <li>Frontend opens Razorpay Checkout with that order id + Key ID; the
 *       customer pays via UPI (Google Pay / PhonePe / QR), card, etc.</li>
 *   <li>Checkout returns razorpay_payment_id + razorpay_signature; the backend
 *       verifies the signature here before marking the order paid.</li>
 * </ol>
 *
 * <p>The Key Secret stays on the backend (environment variable) and is used
 * only to create orders and to verify signatures. It is never sent to the
 * client and must never be committed to Git. A frontend success callback alone
 * is never trusted: {@link #verifyPayment} re-checks the HMAC signature.
 */
@Component
@ConditionalOnProperty(name = "kisanfarm.payment.provider", havingValue = "razorpay")
public class RazorpayPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentProvider.class);

    @Value("${kisanfarm.payment.razorpay.key-id:}")
    private String keyId;

    @Value("${kisanfarm.payment.razorpay.key-secret:}")
    private String keySecret;

    private RazorpayClient client;

    @PostConstruct
    void init() {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new IllegalStateException(
                    "Razorpay provider active but RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET are not set.");
        }
        try {
            client = new RazorpayClient(keyId, keySecret);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Razorpay client: " + e.getMessage(), e);
        }
        // Log only whether a live or test key is in use, never the secret.
        log.info("Razorpay payment provider initialized ({} mode).",
                keyId.startsWith("rzp_live") ? "LIVE" : "TEST");
    }

    @Override
    public String name() {
        return "razorpay";
    }

    @Override
    public CreateResult createPayment(String orderNumber, BigDecimal amount, String currency) {
        try {
            // Razorpay expects the amount in the smallest currency unit (paise for INR).
            long minor = amount.multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP).longValueExact();

            JSONObject options = new JSONObject();
            options.put("amount", minor);
            options.put("currency", currency.toUpperCase());
            options.put("receipt", orderNumber);
            JSONObject notes = new JSONObject();
            notes.put("order_number", orderNumber);
            options.put("notes", notes);

            Order order = client.orders.create(options);
            String razorpayOrderId = order.get("id");

            // clientSecret carries the PUBLIC Key ID the frontend needs to open
            // Checkout. The actual order id is the providerPaymentId.
            return new CreateResult(razorpayOrderId, keyId);
        } catch (Exception e) {
            throw new RuntimeException("Razorpay order creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies the HMAC-SHA256 signature Razorpay returns on a successful
     * payment. The signature is computed by Razorpay over
     * {@code razorpay_order_id + "|" + razorpay_payment_id} using the Key Secret;
     * we recompute it server-side and compare. This is the authoritative proof
     * of payment.
     *
     * @param confirmation            razorpay_payment_id + razorpay_signature from the client
     * @param storedProviderPaymentId the Razorpay order id saved at create time
     */
    @Override
    public boolean verifyPayment(Confirmation confirmation, String storedProviderPaymentId) {
        if (confirmation == null
                || confirmation.providerPaymentId() == null || confirmation.providerPaymentId().isBlank()
                || confirmation.razorpaySignature() == null || confirmation.razorpaySignature().isBlank()
                || storedProviderPaymentId == null || storedProviderPaymentId.isBlank()) {
            log.warn("Razorpay verify called with missing fields.");
            return false;
        }
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", storedProviderPaymentId);
            attributes.put("razorpay_payment_id", confirmation.providerPaymentId());
            attributes.put("razorpay_signature", confirmation.razorpaySignature());
            // Throws RazorpayException if the signature does not match.
            return Utils.verifyPaymentSignature(attributes, keySecret);
        } catch (Exception e) {
            log.warn("Razorpay signature verification failed for order {}: {}",
                    storedProviderPaymentId, e.getMessage());
            return false;
        }
    }
}
