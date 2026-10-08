package com.kisanfarm.daoimp;

import com.kisanfarm.dao.UserDao;
import com.kisanfarm.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDaoImpl implements UserDao {

    private final JdbcTemplate jdbc;

    public UserDaoImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<User> ROW_MAPPER = (rs, n) -> new User(
            rs.getLong("id"),
            rs.getString("mobile"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("role"),
            rs.getBoolean("active"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()
    );

    @Override
    public Optional<User> findByMobile(String mobile) {
        List<User> list = jdbc.query("SELECT * FROM users WHERE mobile = ?", ROW_MAPPER, mobile);
        return list.stream().findFirst();
    }

    @Override
    public Long save(User u) {
        String sql = "INSERT INTO users (mobile, name, email, role, active) VALUES (?, ?, ?, ?, ?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, u.getMobile());
            ps.setString(2, u.getName());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getRole() != null ? u.getRole() : "CUSTOMER");
            ps.setBoolean(5, u.isActive());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key != null ? key.longValue() : null;
    }
}
