package com.kisanfarm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Development OTP provider. Generates a 6-digit code, logs it to the console
 * (so you can test without Twilio), and stores only a hash. Enabled when
 * kisanfarm.otp.provider=mock (the default).
 */
@Component
@ConditionalOnProperty(name = "kisanfarm.otp.provider", havingValue = "mock", matchIfMissing = true)
public class MockOtpProvider implements OtpProvider {

    private static final Logger log = LoggerFactory.getLogger(MockOtpProvider.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public SendResult send(String mobile) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        // Dev convenience: print the OTP. (Never do this in production.)
        log.info("[MOCK OTP] code for mobile ending {} = {}", tail(mobile), code);
        return new SendResult(null, hash(code), code);
    }

    @Override
    public boolean verify(String mobile, String code, SendResult ctx) {
        if (ctx == null || ctx.codeHashOrNull() == null) return false;
        return constantTimeEquals(hash(code), ctx.codeHashOrNull());
    }

    private static String tail(String mobile) {
        return mobile != null && mobile.length() >= 2 ? mobile.substring(mobile.length() - 2) : "**";
    }

    static String hash(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
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
