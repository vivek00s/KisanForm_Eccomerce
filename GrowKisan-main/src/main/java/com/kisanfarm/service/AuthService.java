package com.kisanfarm.service;

import com.kisanfarm.dao.OtpRequestDao;
import com.kisanfarm.dao.UserDao;
import com.kisanfarm.model.OtpRequest;
import com.kisanfarm.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Orchestrates the OTP login flow: send + verify, with rate limiting, expiry,
 * attempt limits, user creation, and token issuance. Delegates OTP delivery to
 * the configured {@link OtpProvider} (2Factor.in).
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final OtpProvider otpProvider;
    private final OtpRequestDao otpDao;
    private final UserDao userDao;

    @Value("${kisanfarm.otp.expiry-seconds:300}")
    private long expirySeconds;

    @Value("${kisanfarm.otp.max-sends-per-hour:5}")
    private int maxSendsPerHour;

    @Value("${kisanfarm.otp.max-verify-attempts:5}")
    private int maxVerifyAttempts;

    // Per-IP hourly cap. Higher than the per-mobile cap to tolerate shared
    // networks (offices, campuses, carrier NAT) where many legitimate users
    // share one public IP, while still blocking a single source spraying OTPs
    // across many different numbers.
    @Value("${kisanfarm.otp.max-sends-per-ip-per-hour:30}")
    private int maxSendsPerIpPerHour;

    private static final SecureRandom RANDOM = new SecureRandom();

    public AuthService(OtpProvider otpProvider, OtpRequestDao otpDao, UserDao userDao) {
        this.otpProvider = otpProvider;
        this.otpDao = otpDao;
        this.userDao = userDao;
    }

    public static class AuthException extends RuntimeException {
        public AuthException(String message) { super(message); }
    }

    /**
     * Send an OTP to the mobile via the OTP provider.
     *
     * <p>Runs in a single transaction. The per-mobile limit is enforced with a
     * row-level lock ({@code countSinceForUpdate}) so two concurrent requests for
     * the same number cannot both slip past the limit — the second blocks on the
     * lock until the first commits its insert, then re-reads the true count. A
     * per-IP hourly cap is also enforced to stop one source from spraying OTPs
     * across many different numbers.
     *
     * @param requestIp client IP (may be null if unavailable); used for the per-IP limit.
     */
    @Transactional
    public SendOtpResult sendOtp(String mobileRaw, String requestIp) {
        String mobile = normalize(mobileRaw);
        String masked = mask(mobile);
        Instant hourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        log.info("send-otp requested for mobile {} (ip {}) via provider '{}'",
                masked, maskIp(requestIp), otpProvider.name());

        // Per-IP limit first (cheap, non-locking). Protects against a single
        // source hitting many different numbers.
        if (requestIp != null && !requestIp.isBlank()) {
            int perIp = otpDao.countByIpSince(requestIp, hourAgo);
            log.debug("send-otp per-IP check for {}: {} sent from ip {} in last hour (limit {})",
                    masked, perIp, maskIp(requestIp), maxSendsPerIpPerHour);
            if (perIp >= maxSendsPerIpPerHour) {
                log.warn("send-otp BLOCKED (per-IP) from ip {}: {} >= {} in last hour",
                        maskIp(requestIp), perIp, maxSendsPerIpPerHour);
                throw new AuthException("Too many OTP requests from your network. Please try again later.");
            }
        }

        // Per-mobile limit, atomic: FOR UPDATE locks the matched rows so two
        // concurrent sends for the same number serialize and cannot both pass.
        int recent = otpDao.countSinceForUpdate(mobile, hourAgo);
        log.debug("send-otp per-mobile check for {}: {} sent in last hour (limit {})", masked, recent, maxSendsPerHour);
        if (recent >= maxSendsPerHour) {
            log.warn("send-otp BLOCKED for {}: rate limit reached ({} >= {} in last hour)",
                    masked, recent, maxSendsPerHour);
            throw new AuthException("Too many OTP requests. Please try again later.");
        }

        OtpProvider.SendResult sr;
        try {
            sr = otpProvider.send(mobile);
        } catch (TwoFactorOtpProvider.OtpDeliveryException e) {
            // Provider could not deliver (network/timeout/API error). Surface a
            // clean, user-facing message instead of a 500. The transaction rolls
            // back, so no half-written row remains.
            log.warn("send-otp FAILED for {}: provider could not deliver: {}", masked, e.getMessage());
            throw new AuthException(e.getMessage());
        }

        OtpRequest row = new OtpRequest();
        row.setMobile(mobile);
        row.setProvider(otpProvider.name());
        row.setProviderRef(sr.providerRef());  // provider session id; raw OTP never stored
        row.setRequestIp(requestIp);
        row.setStatus("PENDING");
        row.setAttempts(0);
        row.setExpiresAt(Instant.now().plusSeconds(expirySeconds));
        Long id = otpDao.save(row);  // insert happens under the lock held above

        log.info("send-otp OK for {}: otp_request id={} saved PENDING, expires in {}s (session acquired)",
                masked, id, expirySeconds);
        return new SendOtpResult(true);
    }

    /**
     * Verify an OTP. On success creates/loads the user and returns a token.
     *
     * <p>Session ownership: the session id used for verification comes from the
     * stored PENDING row for this mobile — it is <b>never</b> taken from the
     * client. The client sends only {mobile, code}.
     *
     * <p>Single-use & concurrency-safe: the PENDING→VERIFIED transition is an
     * atomic conditional UPDATE. Only the first caller to flip a PENDING row
     * updates one row and proceeds; a replay of an already-VERIFIED OTP, or a
     * second concurrent request with the same correct code, updates zero rows
     * and is rejected. A verified OTP can therefore never authenticate twice.
     */
    @Transactional
    public VerifyResult verifyOtp(String mobileRaw, String code) {
        String mobile = normalize(mobileRaw);
        String masked = mask(mobile);
        log.info("verify-otp requested for mobile {}", masked);

        OtpRequest otp = otpDao.findLatestPending(mobile)
                .orElseThrow(() -> {
                    log.warn("verify-otp FAILED for {}: no active (PENDING) OTP found", masked);
                    return new AuthException("No active OTP. Please request a new one.");
                });

        if (otp.getExpiresAt() != null && Instant.now().isAfter(otp.getExpiresAt())) {
            otpDao.updateStatus(otp.getId(), "EXPIRED");
            log.warn("verify-otp FAILED for {}: otp_request id={} expired at {}", masked, otp.getId(), otp.getExpiresAt());
            throw new AuthException("OTP has expired. Please request a new one.");
        }

        if (otp.getAttempts() >= maxVerifyAttempts) {
            otpDao.updateStatus(otp.getId(), "FAILED");
            log.warn("verify-otp FAILED for {}: otp_request id={} hit max attempts ({})",
                    masked, otp.getId(), maxVerifyAttempts);
            throw new AuthException("Too many incorrect attempts. Please request a new OTP.");
        }

        // Session id is loaded from our own row, not supplied by the client.
        OtpProvider.SendResult ctx = new OtpProvider.SendResult(otp.getProviderRef());

        log.debug("verify-otp calling provider for {} (otp_request id={}, attempt {} of {})",
                masked, otp.getId(), otp.getAttempts() + 1, maxVerifyAttempts);
        boolean ok = otpProvider.verify(mobile, code, ctx);
        if (!ok) {
            otpDao.incrementAttempts(otp.getId());
            log.warn("verify-otp REJECTED for {}: provider says code invalid (otp_request id={}, attempt {} of {})",
                    masked, otp.getId(), otp.getAttempts() + 1, maxVerifyAttempts);
            throw new AuthException("Invalid OTP. Please try again.");
        }

        // Atomic single-use claim: only one caller can transition PENDING->VERIFIED.
        int claimed = otpDao.markVerifiedIfPending(otp.getId());
        if (claimed != 1) {
            // Already consumed (replay) or lost a concurrent race. Never issue a token.
            log.warn("verify-otp REJECTED for {}: otp_request id={} already consumed (single-use guard, rows={})",
                    masked, otp.getId(), claimed);
            throw new AuthException("This OTP has already been used. Please request a new one.");
        }
        log.info("verify-otp OK for {}: otp_request id={} atomically marked VERIFIED", masked, otp.getId());

        // Find or create the user.
        boolean[] created = {false};
        User user = userDao.findByMobile(mobile).orElseGet(() -> {
            User u = new User();
            u.setMobile(mobile);
            u.setRole("CUSTOMER");
            u.setActive(true);
            Long id = userDao.save(u);
            u.setId(id);
            created[0] = true;
            return u;
        });
        log.info("verify-otp login success for {}: user id={} ({})",
                masked, user.getId(), created[0] ? "new account created" : "existing account");

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

    /** Mask a mobile for logs: keep first 2 and last 2 digits, hide the middle. */
    private static String mask(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return mobile.substring(0, 2) + "******" + mobile.substring(mobile.length() - 2);
    }

    /** Mask an IP for logs: keep the first octet/segment only. */
    private static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) return "unknown";
        int dot = ip.indexOf('.');
        if (dot > 0) return ip.substring(0, dot) + ".x.x.x";
        int colon = ip.indexOf(':');
        if (colon > 0) return ip.substring(0, colon) + ":…";
        return "x";
    }

    /** Simple opaque token: base64(userId:mobile:random). Swap for JWT later. */
    private String issueToken(User user) {
        byte[] rnd = new byte[24];
        RANDOM.nextBytes(rnd);
        String raw = user.getId() + ":" + user.getMobile() + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public record SendOtpResult(boolean sent) {}
    public record VerifyResult(String token, User user) {}
}
