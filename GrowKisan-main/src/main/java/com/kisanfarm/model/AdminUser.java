package com.kisanfarm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Admin/seller account for username+password login. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUser {
    private Long id;
    private String username;
    private String passwordHash;
    private String name;
}
