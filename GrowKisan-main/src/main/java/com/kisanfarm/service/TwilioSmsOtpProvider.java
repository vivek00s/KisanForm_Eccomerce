package com.kisanfarm.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Twilio OTP via the plain SMS (Messaging) API. Works on a Twilio TRIAL account
 * (no Verify upgrade needed). We generate the 6-digit code ourselves, store only
 * a hash, and send the code as a normal SMS from the configured trial number.
 *
 * Enabled when kisanfarm.otp.provider=twilio-sms. Trial accounts can only send
 * to verified numbers and Twilio prepends a trial notice to the message.
 */
@Component
@ConditionalOnProperty(name = "kisanfarm.otp.provider", havingValue = "twilio-sms")
public class TwilioSmsOtpProvider implements OtpProvider {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsOtpProvider.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${kisanfarm.twilio.account-sid:}")
    private String accountSid;

    @Value("${kisanfarm.twilio.auth-token:}")
    private String authToken;

    @Value("${kisanfarm.twilio.from-number:}")
    private String fromNumber;

    @Value("${kisanfarm.twilio.default-country-code:+91}")
    private String defaultCountryCode;

    @PostConstruct
    void init() {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            throw new IllegalStateException("twilio-sms provider active but TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN not set.");
        }
        if (fromNumber == null || fromNumber.isBlank()) {
            throw new IllegalStateException("twilio-sms provider active but TWILIO_FROM_NUMBER (your trial number) is not set.");
        }
        Twilio.init(accountSid, authToken);
        log.info("Twilio SMS OTP provider initialized (sending from {}).", fromNumber);
    }

    @Override
    public String name() {
        return "twilio-sms";
    }

    @Override
    public SendResult send(String mobile) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        // Twilio TRIAL accounts only accept predefined SMS templates. The
        // verification-code template must read exactly like this.
        String body = "Your verification code is: " + code;

        Message message = Message.creator(
                new PhoneNumber(toE164(mobile)),
                new PhoneNumber(fromNumber),
                body
        ).create();

        // Keep the Twilio message SID as reference; store only a hash of the code.
        log.info("Sent OTP SMS to mobile ending {} (Twilio SID {}).", tail(mobile), message.getSid());
        return new SendResult(message.getSid(), hash(code), null);
    }

    @Override
    public boolean verify(String mobile, String code, SendResult ctx) {
        if (ctx == null || ctx.codeHashOrNull() == null) return false;
        return constantTimeEquals(hash(code), ctx.codeHashOrNull());
    }

    private String toE164(String mobile) {
        if (mobile == null) return null;
        String m = mobile.trim();
        return m.startsWith("+") ? m : defaultCountryCode + m;
    }

    private static String tail(String mobile) {
        return mobile != null && mobile.length() >= 2 ? mobile.substring(mobile.length() - 2) : "**";
    }

    static String hash(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(code.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Hash failure", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
