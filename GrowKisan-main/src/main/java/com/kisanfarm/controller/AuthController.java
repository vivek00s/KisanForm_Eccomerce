package com.kisanfarm.controller;

import com.kisanfarm.model.User;
import com.kisanfarm.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * OTP authentication endpoints.
 *   POST /api/auth/send-otp   { mobile }
 *   POST /api/auth/verify-otp { mobile, code }  -> { token, user }
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> body) {
        String mobile = body.get("mobile");
        AuthService.SendOtpResult result = authService.sendOtp(mobile);
        Map<String, Object> resp = new HashMap<>();
        resp.put("sent", result.sent());
        resp.put("message", "OTP sent successfully.");
        // devCode is only present in mock mode; handy for local testing.
        if (result.devCode() != null) resp.put("devCode", result.devCode());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> body) {
        String mobile = body.get("mobile");
        String code = body.get("code");
        AuthService.VerifyResult result = authService.verifyOtp(mobile, code);

        User u = result.user();
        Map<String, Object> user = new HashMap<>();
        user.put("id", u.getId());
        user.put("mobile", u.getMobile());
        user.put("name", u.getName());
        user.put("role", u.getRole());

        Map<String, Object> resp = new HashMap<>();
        resp.put("token", result.token());
        resp.put("user", user);
        return ResponseEntity.ok(resp);
    }

    // Turn auth failures into clean 400s with a message the UI can show.
    @ExceptionHandler(AuthService.AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuth(AuthService.AuthException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }
}
