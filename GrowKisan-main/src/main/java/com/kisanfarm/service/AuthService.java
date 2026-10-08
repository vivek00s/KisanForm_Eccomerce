package com.kisanfarm.service;

import com.kisanfarm.dao.OtpRequestDao;
import com.kisanfarm.dao.UserDao;
import com.kisanfarm.model.OtpRequest;
import com.kisanfarm.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

/**
 * Orchestrates the OTP login flow: send + verify, with rate limiting, expiry,
 * attempt limits, user creation, and token issuance. Delegates OTP delivery to
 * the configured {@link OtpProvider} (mock or Twilio).
 */
@Service
public class AuthService {

    private final OtpProvider otpProvider;
    private final OtpRequestDao otpDao;
    private final UserDao userDao;

    @Value("${kisanfarm.otp.expiry-seconds:300}")
    private long expirySeconds;

    @Value("${kisanfarm.otp.max-sends-per-hour:5}")
    private int maxSendsPerHour;

    @Value("${kisanfarm.otp.max-verify-attempts:5}")
    private int maxVerifyAttempts;

    private static final SecureRandom RANDOM = new SecureRandom();

    public AuthService(OtpProvider otpProvider, OtpRequestDao otpDao, UserDao userDao) {
        this.otpProvider = otpProvider;
        this.otpDao = otpDao;
        this.userDao = userDao;
    }

    public static class AuthException extends RuntimeException {
        public AuthException(String message) { super(message); }
    }

    /** Send an OTP to the mobile. Returns whether a dev code is available (mock). */
    public SendOtpResult sendOtp(String mobileRaw) {
        String mobile = normalize(mobileRaw);

        // Rate limit: max N sends per hour.
        int recent = otpDao.countSince(mobile, Instant.now().minus(1, ChronoUnit.HOURS));
        if (recent >= maxSendsPerHour) {
            throw new AuthException("Too many OTP requests. Please try again later.");
        }

        OtpProvider.SendResult sr = otpProvider.send(mobile);

        OtpRequest row = new OtpRequest();
        row.setMobile(mobile);
        row.setProvider(otpProvider.name());
        row.setProviderRef(sr.providerRef());
        row.setCodeHash(sr.codeHashOrNull());
        row.setStatus("PENDING");
        row.setAttempts(0);
        row.setExpiresAt(Instant.now().plusSeconds(expirySeconds));
        otpDao.save(row);

        // devCode is only non-null for the mock provider (never in production).
        return new SendOtpResult(true, sr.devCodeOrNull());
    }

    /** Verify an OTP. On success creates/loads the user and returns a token. */
    public VerifyResult verifyOtp(String mobileRaw, String code) {
        String mobile = normalize(mobileRaw);

        OtpRequest otp = otpDao.findLatestPending(mobile)
                .orElseThrow(() -> new AuthException("No active OTP. Please request a new one."));

        if (otp.getExpiresAt() != null && Instant.now().isAfter(otp.getExpiresAt())) {
            otpDao.updateStatus(otp.getId(), "EXPIRED");
            throw new AuthException("OTP has expired. Please request a new one.");
        }

        if (otp.getAttempts() >= maxVerifyAttempts) {
            otpDao.updateStatus(otp.getId(), "FAILED");
            throw new AuthException("Too many incorrect attempts. Please request a new OTP.");
        }

        OtpProvider.SendResult ctx = new OtpProvider.SendResult(
                otp.getProviderRef(), otp.getCodeHash(), null);

        boolean ok = otpProvider.verify(mobile, code, ctx);
        if (!ok) {
            otpDao.incrementAttempts(otp.getId());
            throw new AuthException("Invalid OTP. Please try again.");
        }

        otpDao.updateStatus(otp.getId(), "VERIFIED");

        // Find or create the user.
        User user = userDao.findByMobile(mobile).orElseGet(() -> {
            User u = new User();
            u.setMobile(mobile);
            u.setRole("CUSTOMER");
            u.setActive(true);
            Long id = userDao.save(u);
            u.setId(id);
            return u;
        });

        String token = issueToken(user);
        return new VerifyResult(token, user);
    }

    // --- helpers ---

    private String normalize(String mobile) {
        if (mobile == null) throw new AuthException("Mobile number is required.");
        String digits = mobile.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) digits = digits.substring(2);
        if (digits.length() != 10) throw new AuthException("Please enter a valid 10-digit mobile number.");
        return digits;
    }

    /** Simple opaque token: base64(userId:mobile:random). Swap for JWT later. */
    private String issueToken(User user) {
        byte[] rnd = new byte[24];
        RANDOM.nextBytes(rnd);
        String raw = user.getId() + ":" + user.getMobile() + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public record SendOtpResult(boolean sent, String devCode) {}
    public record VerifyResult(String token, User user) {}
}
