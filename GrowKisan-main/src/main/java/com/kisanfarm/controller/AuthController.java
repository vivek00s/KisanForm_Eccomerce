package com.kisanfarm.controller;

import com.kisanfarm.model.User;
import com.kisanfarm.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> body,
                                                       HttpServletRequest request) {
        String mobile = body.get("mobile");
        String clientIp = clientIp(request);
        AuthService.SendOtpResult result = authService.sendOtp(mobile, clientIp);
        Map<String, Object> resp = new HashMap<>();
        resp.put("sent", result.sent());
        resp.put("message", "OTP sent successfully.");
        return ResponseEntity.ok(resp);
    }

    /**
     * Best-effort client IP. Honours the first hop in X-Forwarded-For when the
     * app sits behind a reverse proxy/load balancer; otherwise uses the socket
     * address. NOTE: X-Forwarded-For is client-settable, so this is a rate-limit
     * heuristic, not a security boundary — in production the edge proxy should
     * overwrite/append it and you should trust only the proxy-set value.
     */
    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // First address is the original client.
            String first = xff.split(",")[0].trim();
            if (!first.isBlank()) return first;
        }
        return request.getRemoteAddr();
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
