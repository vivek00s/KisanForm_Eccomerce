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
}
