package com.kisanfarm.dao;

import com.kisanfarm.model.User;

import java.util.Optional;

public interface UserDao {
    Optional<User> findByMobile(String mobile);
    Long save(User user);
}
