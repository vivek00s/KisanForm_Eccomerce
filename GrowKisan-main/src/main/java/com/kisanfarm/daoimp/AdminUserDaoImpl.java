package com.kisanfarm.daoimp;

import com.kisanfarm.dao.AdminUserDao;
import com.kisanfarm.model.AdminUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AdminUserDaoImpl implements AdminUserDao {

    private final JdbcTemplate jdbc;

    public AdminUserDaoImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<AdminUser> ROW_MAPPER = (rs, n) -> new AdminUser(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("name")
    );

    @Override
    public Optional<AdminUser> findByUsername(String username) {
        List<AdminUser> list = jdbc.query(
                "SELECT * FROM admin_users WHERE username = ?", ROW_MAPPER, username);
        return list.stream().findFirst();
    }
}
