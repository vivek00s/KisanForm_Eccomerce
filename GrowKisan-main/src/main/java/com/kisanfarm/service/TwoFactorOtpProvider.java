package com.kisanfarm.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Production OTP provider backed by 2Factor.in (https://2factor.in).
 * Enabled when {@code kisanfarm.otp.provider=twofactor}.
 *
 * <p>Flow:
 * <ul>
 *   <li><b>send</b> calls the AUTOGEN endpoint
 *       {@code GET /API/V1/{key}/SMS/{phone}/AUTOGEN}. 2Factor generates the OTP,
 *       sends the SMS, and returns a <i>Session ID</i> in the {@code Details}
 *       field. We keep only that session id as the provider reference; the raw
 *       OTP never touches our backend.</li>
 *   <li><b>verify</b> calls {@code GET /API/V1/{key}/SMS/VERIFY/{sessionId}/{otp}}.
 *       2Factor returns {@code Status=Success} and {@code Details=OTP Matched}
 *       when the code is correct.</li>
 * </ul>
 *
 * <p>The API key is read from backend configuration ({@code TWOFACTOR_API_KEY})
 * and is never exposed to the frontend. The OTP length (6 digits) is controlled
 * by the SMS template configured in the 2Factor dashboard.
 */
@Component
@ConditionalOnProperty(name = "kisanfarm.otp.provider", havingValue = "twofactor")
public class TwoFactorOtpProvider implements OtpProvider {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorOtpProvider.class);

    private final String apiKey;
    private final String baseUrl;
    private final String defaultCountryCode;
    private final RestClient restClient;

    /**
     * Spring injects a {@link RestClient.Builder}. In production
     * {@link TwoFactorConfig} pre-configures it with connect/read timeouts; tests
     * can pass a builder bound to a {@code MockRestServiceServer}. We only set the
     * base URL here so the test's mock request factory is never overridden.
     */
    public TwoFactorOtpProvider(
            @Value("${kisanfarm.twofactor.api-key:}") String apiKey,
            @Value("${kisanfarm.twofactor.base-url:https://2factor.in/API/V1}") String baseUrl,
            @Value("${kisanfarm.twofactor.default-country-code:+91}") String defaultCountryCode,
            @Qualifier("twoFactorRestClientBuilder") RestClient.Builder restClientBuilder) {
        this.apiKey = apiKey;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.defaultCountryCode = defaultCountryCode;
        this.restClient = restClientBuilder
                .baseUrl(this.baseUrl)
                .build();
    }

    @PostConstruct
    void init() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "2Factor OTP provider is active but TWOFACTOR_API_KEY is not set.");
        }
        log.info("2Factor OTP provider initialized (base URL {}).", baseUrl);
    }

    @Override
    public String name() {
        return "twofactor";
    }

    /**
     * Trigger 2Factor AUTOGEN. 2Factor owns and sends the OTP; we persist only
     * the returned session id (as providerRef). No raw code or hash is stored.
     */
    @Override
    public SendResult send(String mobile) {
        String phone = toInternational(mobile);
        TwoFactorResponse body;
        try {
            body = restClient.get()
                    .uri("/{key}/SMS/{phone}/AUTOGEN", apiKey, phone)
                    .retrieve()
                    .body(TwoFactorResponse.class);
        } catch (RestClientException e) {
            // Network error, timeout, or non-2xx from 2Factor.
            log.warn("2Factor AUTOGEN call failed for mobile ending {}: {}", tail(mobile), e.getMessage());
            throw new OtpDeliveryException("Could not send OTP right now. Please try again.", e);
        }

        if (body == null || !isSuccess(body.status())) {
            String detail = body != null ? body.details() : "no response body";
            log.warn("2Factor AUTOGEN returned non-success for mobile ending {}: {}", tail(mobile), detail);
            throw new OtpDeliveryException("Could not send OTP right now. Please try again.");
        }

        String sessionId = body.details();
        if (sessionId == null || sessionId.isBlank()) {
            throw new OtpDeliveryException("OTP provider did not return a session id.");
        }
        log.info("2Factor OTP sent to mobile ending {} (session acquired).", tail(mobile));
        // providerRef = session id; the provider owns the OTP, nothing else is stored.
        return new SendResult(sessionId);
    }

    /**
     * Verify the user-entered code against the 2Factor session id captured at
     * send time. Any provider error or mismatch is treated as a failed check
     * (never a bypass).
     */
    @Override
    public boolean verify(String mobile, String code, SendResult ctx) {
        if (ctx == null || ctx.providerRef() == null || ctx.providerRef().isBlank()) {
            // No session id to verify against -> cannot approve.
            return false;
        }
        if (code == null || code.isBlank()) {
            return false;
        }

        TwoFactorResponse body;
        try {
            body = restClient.get()
                    .uri("/{key}/SMS/VERIFY/{sessionId}/{otp}", apiKey, ctx.providerRef(), code.trim())
                    .retrieve()
                    .body(TwoFactorResponse.class);
        } catch (RestClientException e) {
            // 2Factor returns HTTP 200 with Status=Error for a wrong/expired OTP,
            // so a thrown exception here means a transport/5xx problem. Fail closed.
            log.warn("2Factor VERIFY call failed for mobile ending {}: {}", tail(mobile), e.getMessage());
            return false;
        }

        if (body == null) return false;
        boolean approved = isSuccess(body.status());
        if (!approved) {
            log.debug("2Factor VERIFY rejected code for mobile ending {}: {}", tail(mobile), body.details());
        }
        return approved;
    }

    // --- helpers ---

    private static boolean isSuccess(String status) {
        return status != null && status.trim().equalsIgnoreCase("Success");
    }

    /**
     * 2Factor accepts Indian numbers as 10 digits or with the country code. We
     * send the full international form (without the '+') to be explicit.
     */
    private String toInternational(String mobile) {
        if (mobile == null) return null;
        String digits = mobile.trim().replaceAll("\\D", "");
        String cc = defaultCountryCode.replaceAll("\\D", "");
        if (digits.length() == 10) {
            return cc + digits;
        }
        // Already includes a country code (e.g. 91XXXXXXXXXX).
        return digits;
    }

    private static String tail(String mobile) {
        return mobile != null && mobile.length() >= 2 ? mobile.substring(mobile.length() - 2) : "**";
    }

    private static String stripTrailingSlash(String url) {
        if (url == null) return null;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Thrown when the provider cannot deliver an OTP (network, timeout, API error). */
    public static class OtpDeliveryException extends RuntimeException {
        public OtpDeliveryException(String message) { super(message); }
        public OtpDeliveryException(String message, Throwable cause) { super(message, cause); }
    }

    /**
     * 2Factor JSON response shape: {"Status":"Success","Details":"<session id or message>"}.
     * Field names are capitalised in the API; map them explicitly and ignore extras.
     */
    record TwoFactorResponse(String status, String details) {
        @com.fasterxml.jackson.annotation.JsonCreator
        TwoFactorResponse(
                @com.fasterxml.jackson.annotation.JsonProperty("Status") String status,
                @com.fasterxml.jackson.annotation.JsonProperty("Details") String details) {
            this.status = status;
            this.details = details;
        }
    }
}
