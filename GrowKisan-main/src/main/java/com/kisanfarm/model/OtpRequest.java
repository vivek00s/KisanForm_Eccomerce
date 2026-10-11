package com.kisanfarm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** OTP request record. The raw OTP is never stored; 2Factor owns it. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequest {
    private Long id;
    private String mobile;
    private String provider;     // twofactor
    private String providerRef;  // 2Factor session id
    private String codeHash;     // retained (nullable) for schema compatibility; always null now
    private String requestIp;    // client IP captured at send time (per-IP rate limiting)
    private String status;       // PENDING | VERIFIED | EXPIRED | FAILED
    private int attempts;
    private Instant expiresAt;
    private Instant createdAt;
}
