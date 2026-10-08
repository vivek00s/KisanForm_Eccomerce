package com.kisanfarm.service;

import com.kisanfarm.dao.AdminUserDao;
import com.kisanfarm.model.AdminUser;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Admin username+password login. Passwords are stored as SHA-256 hashes.
 * (For production, upgrade to BCrypt; SHA-256 is used here for a simple seed.)
 */
@Service
public class AdminAuthService {

    private final AdminUserDao adminDao;
    private static final SecureRandom RANDOM = new SecureRandom();

    public AdminAuthService(AdminUserDao adminDao) {
        this.adminDao = adminDao;
    }

    public static class AdminAuthException extends RuntimeException {
        public AdminAuthException(String message) { super(message); }
    }

    public LoginResult login(String username, String password) {
        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            throw new AdminAuthException("Username and password are required.");
        }
        AdminUser admin = adminDao.findByUsername(username.trim())
                .orElseThrow(() -> new AdminAuthException("Invalid username or password."));

        if (!constantTimeEquals(sha256(password), admin.getPasswordHash())) {
            throw new AdminAuthException("Invalid username or password.");
        }

        String token = issueToken(admin);
        return new LoginResult(token, admin);
    }

    private String issueToken(AdminUser admin) {
        byte[] rnd = new byte[24];
        RANDOM.nextBytes(rnd);
        String raw = "ADMIN:" + admin.getId() + ":" + admin.getUsername() + ":"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
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

    public record LoginResult(String token, AdminUser admin) {}
}
