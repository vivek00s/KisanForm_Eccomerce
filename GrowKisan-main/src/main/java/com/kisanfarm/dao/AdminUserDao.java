package com.kisanfarm.dao;

import com.kisanfarm.model.AdminUser;

import java.util.Optional;

public interface AdminUserDao {
    Optional<AdminUser> findByUsername(String username);
}
