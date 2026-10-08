package com.kisanfarm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Plain user model (no JPA). Maps to the 'users' table via JDBC. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String mobile;
    private String name;
    private String email;
    private String role;    // CUSTOMER | ADMIN
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
