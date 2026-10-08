package com.kisanfarm.daoimp;

import com.kisanfarm.dao.PaymentDao;
import com.kisanfarm.model.Payment;
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
public class PaymentDaoImpl implements PaymentDao {

    private final JdbcTemplate jdbc;

    public PaymentDaoImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Payment> ROW_MAPPER = (rs, n) -> new Payment(
            rs.getLong("id"),
            rs.getLong("order_id"),
            rs.getString("provider"),
            rs.getString("provider_payment_id"),
            rs.getString("provider_order_id"),
            rs.getString("status"),
            rs.getBigDecimal("amount"),
            rs.getString("currency"),
            rs.getString("client_secret"),
            rs.getString("failure_reason")
    );

    @Override
    public Long save(Payment p) {
        String sql = "INSERT INTO payments (order_id, provider, provider_payment_id, provider_order_id, " +
                "status, amount, currency, client_secret, failure_reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, p.getOrderId());
            ps.setString(2, p.getProvider());
            ps.setString(3, p.getProviderPaymentId());
            ps.setString(4, p.getProviderOrderId());
            ps.setString(5, p.getStatus() != null ? p.getStatus() : "PENDING");
            ps.setBigDecimal(6, p.getAmount());
            ps.setString(7, p.getCurrency() != null ? p.getCurrency() : "INR");
            ps.setString(8, p.getClientSecret());
            ps.setString(9, p.getFailureReason());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key != null ? key.longValue() : null;
    }

    @Override
    public Optional<Payment> findByOrderId(Long orderId) {
        List<Payment> list = jdbc.query("SELECT * FROM payments WHERE order_id = ?", ROW_MAPPER, orderId);
        return list.stream().findFirst();
    }

    @Override
    public Optional<Payment> findByProviderPaymentId(String providerPaymentId) {
        List<Payment> list = jdbc.query("SELECT * FROM payments WHERE provider_payment_id = ?", ROW_MAPPER, providerPaymentId);
        return list.stream().findFirst();
    }

    @Override
    public int updateStatus(Long id, String status, String providerPaymentId, String failureReason) {
        return jdbc.update("UPDATE payments SET status = ?, provider_payment_id = COALESCE(?, provider_payment_id), failure_reason = ? WHERE id = ?",
                status, providerPaymentId, failureReason, id);
    }
}
