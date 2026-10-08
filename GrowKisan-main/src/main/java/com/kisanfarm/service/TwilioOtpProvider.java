package com.kisanfarm.service;

import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Production OTP provider backed by Twilio Verify. Enabled when
 * kisanfarm.otp.provider=twilio. All Twilio credentials come from the backend
 * environment and are never exposed to the frontend. Twilio holds the OTP, so
 * we never store the raw code.
 */
@Component
@ConditionalOnProperty(name = "kisanfarm.otp.provider", havingValue = "twilio")
public class TwilioOtpProvider implements OtpProvider {

    private static final Logger log = LoggerFactory.getLogger(TwilioOtpProvider.class);

    @Value("${kisanfarm.twilio.account-sid:}")
    private String accountSid;

    @Value("${kisanfarm.twilio.auth-token:}")
    private String authToken;

    @Value("${kisanfarm.twilio.verify-service-sid:}")
    private String verifyServiceSid;

    @Value("${kisanfarm.twilio.default-country-code:+91}")
    private String defaultCountryCode;

    @PostConstruct
    void init() {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            throw new IllegalStateException("Twilio OTP provider is active but TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN are not set.");
        }
        Twilio.init(accountSid, authToken);
        log.info("Twilio OTP provider initialized (Verify service configured).");
    }

    @Override
    public String name() {
        return "twilio";
    }

    @Override
    public SendResult send(String mobile) {
        Verification verification = Verification
                .creator(verifyServiceSid, toE164(mobile), "sms")
                .create();
        // Twilio owns the OTP; we only keep its SID. No raw code stored.
        return new SendResult(verification.getSid(), null, null);
    }

    @Override
    public boolean verify(String mobile, String code, SendResult ctx) {
        VerificationCheck check = VerificationCheck
                .creator(verifyServiceSid)
                .setTo(toE164(mobile))
                .setCode(code)
                .create();
        return "approved".equalsIgnoreCase(check.getStatus());
    }

    private String toE164(String mobile) {
        if (mobile == null) return null;
        String m = mobile.trim();
        return m.startsWith("+") ? m : defaultCountryCode + m;
    }
}
