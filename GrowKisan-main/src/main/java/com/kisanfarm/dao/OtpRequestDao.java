package com.kisanfarm.dao;

import com.kisanfarm.model.OtpRequest;

import java.time.Instant;
import java.util.Optional;

public interface OtpRequestDao {
    Long save(OtpRequest otp);

    /** Latest PENDING OTP for a mobile, if any. */
    Optional<OtpRequest> findLatestPending(String mobile);

    int updateStatus(Long id, String status);

    int incrementAttempts(Long id);

    /** Count OTP sends for a mobile since the given time (rate limiting). */
    int countSince(String mobile, Instant since);

    /**
     * Count sends for a mobile since the given time, taking a row-level lock on
     * the matched rows (SELECT ... FOR UPDATE). Must be called inside a
     * transaction. Serializes concurrent sends for the same mobile so the
     * per-mobile limit cannot be exceeded by a race.
     */
    int countSinceForUpdate(String mobile, Instant since);

    /** Count sends from an IP since the given time (per-IP rate limiting). */
    int countByIpSince(String requestIp, Instant since);

    /**
     * Atomically mark a request VERIFIED only if it is still PENDING.
     * @return number of rows updated: 1 if this call won the transition,
     *         0 if it was already VERIFIED/EXPIRED/FAILED (lost the race or reused).
     */
    int markVerifiedIfPending(Long id);
}
