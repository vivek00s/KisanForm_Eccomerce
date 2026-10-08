package com.kisanfarm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** OTP request record. Never stores the raw OTP (hash only for mock provider). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequest {
    private Long id;
    private String mobile;
    private String provider;     // mock | twilio
    private String providerRef;  // Twilio Verify SID, if used
    private String codeHash;     // hash of OTP (mock only); null for Twilio
    private String status;       // PENDING | VERIFIED | EXPIRED | FAILED
    private int attempts;
    private Instant expiresAt;
    private Instant createdAt;
}
