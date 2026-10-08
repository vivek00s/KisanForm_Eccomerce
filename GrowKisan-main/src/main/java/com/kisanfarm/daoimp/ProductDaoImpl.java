package com.kisanfarm.daoimp;

import com.kisanfarm.dao.ProductDao;
import com.kisanfarm.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * JDBC Template implementation of {@link ProductDao}.
 * Uses parameterized queries (no string concatenation) to avoid SQL injection.
 */
@Repository
public class ProductDaoImpl implements ProductDao {

    private final JdbcTemplate jdbc;

    public ProductDaoImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Product> ROW_MAPPER = (rs, rowNum) -> new Product(
            rs.getLong("id"),
            rs.getString("sku"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getObject("category_id", Long.class),
            rs.getString("brand"),
            rs.getString("image_url"),
            rs.getBigDecimal("price"),
            rs.getBigDecimal("discount_percent"),
            rs.getBigDecimal("rating"),
            rs.getObject("review_count", Integer.class),
            rs.getString("badge"),
            rs.getString("status"),
            rs.getObject("stock_quantity", Integer.class),
            rs.getBoolean("offer_active"),
            rs.getString("offer_label"),
            rs.getBigDecimal("offer_price")
    );

    @Override
    public List<Product> findAll() {
        return jdbc.query("SELECT * FROM products ORDER BY id DESC", ROW_MAPPER);
    }

    @Override
    public Optional<Product> findById(Long id) {
        List<Product> list = jdbc.query("SELECT * FROM products WHERE id = ?", ROW_MAPPER, id);
        return list.stream().findFirst();
    }

    @Override
    public Long save(Product p) {
        String sql = "INSERT INTO products " +
                "(sku, name, description, category_id, brand, image_url, price, discount_percent, " +
                " rating, review_count, badge, status, stock_quantity, offer_active, offer_label, offer_price) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.getSku());
            ps.setString(2, p.getName());
            ps.setString(3, p.getDescription());
            ps.setObject(4, p.getCategoryId());
            ps.setString(5, p.getBrand());
            ps.setString(6, p.getImageUrl());
            ps.setBigDecimal(7, p.getPrice());
            ps.setBigDecimal(8, nz(p.getDiscountPercent()));
            ps.setBigDecimal(9, p.getRating());
            ps.setObject(10, p.getReviewCount() != null ? p.getReviewCount() : 0);
            ps.setString(11, p.getBadge());
            ps.setString(12, p.getStatus() != null ? p.getStatus() : "ACTIVE");
            ps.setObject(13, p.getStockQuantity() != null ? p.getStockQuantity() : 0);
            ps.setBoolean(14, Boolean.TRUE.equals(p.getOfferActive()));
            ps.setString(15, p.getOfferLabel());
            ps.setBigDecimal(16, p.getOfferPrice());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    @Override
    public int update(Product p) {
        String sql = "UPDATE products SET sku=?, name=?, description=?, category_id=?, brand=?, image_url=?, " +
                "price=?, discount_percent=?, rating=?, review_count=?, badge=?, status=?, " +
                "stock_quantity=?, offer_active=?, offer_label=?, offer_price=? WHERE id=?";
        return jdbc.update(sql,
                p.getSku(), p.getName(), p.getDescription(), p.getCategoryId(), p.getBrand(), p.getImageUrl(),
                p.getPrice(), nz(p.getDiscountPercent()), p.getRating(),
                p.getReviewCount() != null ? p.getReviewCount() : 0,
                p.getBadge(), p.getStatus() != null ? p.getStatus() : "ACTIVE",
                p.getStockQuantity() != null ? p.getStockQuantity() : 0,
                Boolean.TRUE.equals(p.getOfferActive()), p.getOfferLabel(), p.getOfferPrice(),
                p.getId());
    }

    @Override
    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM products WHERE id = ?", id);
    }

    private static java.math.BigDecimal nz(java.math.BigDecimal v) {
        return v != null ? v : java.math.BigDecimal.ZERO;
    }
}
