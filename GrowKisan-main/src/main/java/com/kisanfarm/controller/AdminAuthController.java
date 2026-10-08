package com.kisanfarm.controller;

import com.kisanfarm.model.AdminUser;
import com.kisanfarm.service.AdminAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin login endpoint. POST /api/admin/login { username, password } -> { token, admin }
 */
@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        AdminAuthService.LoginResult result =
                adminAuthService.login(body.get("username"), body.get("password"));

        AdminUser a = result.admin();
        Map<String, Object> admin = new HashMap<>();
        admin.put("id", a.getId());
        admin.put("username", a.getUsername());
        admin.put("name", a.getName());

        Map<String, Object> resp = new HashMap<>();
        resp.put("token", result.token());
        resp.put("admin", admin);
        return ResponseEntity.ok(resp);
    }

    @ExceptionHandler(AdminAuthService.AdminAuthException.class)
    public ResponseEntity<Map<String, String>> handle(AdminAuthService.AdminAuthException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", ex.getMessage()));
    }
}
