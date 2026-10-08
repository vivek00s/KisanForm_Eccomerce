package com.kisanfarm.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Lightweight validation for admin tokens issued by {@link AdminAuthService}.
 * A valid token base64-decodes to a string starting with "ADMIN:".
 * (Sufficient for the hardcoded-admin stage; replace with JWT/session later.)
 */
@Component
public class AdminTokenGuard {

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }

    public boolean isValid(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            return raw.startsWith("ADMIN:");
        } catch (Exception e) {
            return false;
        }
    }

    /** Throws if the token is not a valid admin token. */
    public void require(String token) {
        if (!isValid(token)) {
            throw new ForbiddenException("Admin authorization required.");
        }
    }
}
