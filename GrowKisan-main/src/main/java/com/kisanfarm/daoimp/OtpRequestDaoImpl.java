package com.kisanfarm.daoimp;

import com.kisanfarm.dao.OtpRequestDao;
import com.kisanfarm.model.OtpRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class OtpRequestDaoImpl implements OtpRequestDao {

    private final JdbcTemplate jdbc;

    public OtpRequestDaoImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<OtpRequest> ROW_MAPPER = (rs, n) -> new OtpRequest(
            rs.getLong("id"),
            rs.getString("mobile"),
            rs.getString("provider"),
            rs.getString("provider_ref"),
            rs.getString("code_hash"),
            rs.getString("status"),
            rs.getInt("attempts"),
            rs.getTimestamp("expires_at") != null ? rs.getTimestamp("expires_at").toInstant() : null,
            rs.getTimestamp("created_at").toInstant()
    );

    @Override
    public Long save(OtpRequest o) {
        String sql = "INSERT INTO otp_requests (mobile, provider, provider_ref, code_hash, status, attempts, expires_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, o.getMobile());
            ps.setString(2, o.getProvider());
            ps.setString(3, o.getProviderRef());
            ps.setString(4, o.getCodeHash());
            ps.setString(5, o.getStatus() != null ? o.getStatus() : "PENDING");
            ps.setInt(6, o.getAttempts());
            ps.setTimestamp(7, o.getExpiresAt() != null ? Timestamp.from(o.getExpiresAt()) : null);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key != null ? key.longValue() : null;
    }

    @Override
    public Optional<OtpRequest> findLatestPending(String mobile) {
        List<OtpRequest> list = jdbc.query(
                "SELECT * FROM otp_requests WHERE mobile = ? AND status = 'PENDING' ORDER BY id DESC LIMIT 1",
                ROW_MAPPER, mobile);
        return list.stream().findFirst();
    }

    @Override
    public int updateStatus(Long id, String status) {
        return jdbc.update("UPDATE otp_requests SET status = ? WHERE id = ?", status, id);
    }

    @Override
    public int incrementAttempts(Long id) {
        return jdbc.update("UPDATE otp_requests SET attempts = attempts + 1 WHERE id = ?", id);
    }

    @Override
    public int countSince(String mobile, Instant since) {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM otp_requests WHERE mobile = ? AND created_at >= ?",
                Integer.class, mobile, Timestamp.from(since));
        return c != null ? c : 0;
    }
}
