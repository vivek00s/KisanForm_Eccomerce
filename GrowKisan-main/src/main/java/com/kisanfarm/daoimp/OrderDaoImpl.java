package com.kisanfarm.daoimp;

import com.kisanfarm.dao.OrderDao;
import com.kisanfarm.model.Order;
import com.kisanfarm.model.OrderItem;
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
public class OrderDaoImpl implements OrderDao {

    private final JdbcTemplate jdbc;

    public OrderDaoImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Order> ORDER_MAPPER = (rs, n) -> {
        Order o = new Order();
        o.setId(rs.getLong("id"));
        o.setOrderNumber(rs.getString("order_number"));
        o.setUserMobile(rs.getString("user_mobile"));
        o.setStatus(rs.getString("status"));
        o.setPaymentStatus(rs.getString("payment_status"));
        o.setPaymentMethod(rs.getString("payment_method"));
        o.setSubtotal(rs.getBigDecimal("subtotal"));
        o.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        o.setDeliveryCharge(rs.getBigDecimal("delivery_charge"));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setShipContactName(rs.getString("ship_contact_name"));
        o.setShipContactMobile(rs.getString("ship_contact_mobile"));
        o.setShipLine1(rs.getString("ship_line1"));
        o.setShipLine2(rs.getString("ship_line2"));
        o.setShipCity(rs.getString("ship_city"));
        o.setShipState(rs.getString("ship_state"));
        o.setShipPincode(rs.getString("ship_pincode"));
        o.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        o.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return o;
    };

    private static final RowMapper<OrderItem> ITEM_MAPPER = (rs, n) -> new OrderItem(
            rs.getLong("id"), rs.getLong("order_id"), rs.getLong("product_id"),
            rs.getString("product_name"), rs.getBigDecimal("unit_price"),
            rs.getInt("quantity"), rs.getBigDecimal("line_total"));

    @Override
    public Long save(Order o) {
        String sql = "INSERT INTO orders (order_number, user_mobile, status, payment_status, payment_method, " +
                "subtotal, discount_amount, delivery_charge, total_amount, ship_contact_name, ship_contact_mobile, " +
                "ship_line1, ship_line2, ship_city, ship_state, ship_pincode) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, o.getOrderNumber());
            ps.setString(2, o.getUserMobile());
            ps.setString(3, o.getStatus());
            ps.setString(4, o.getPaymentStatus());
            ps.setString(5, o.getPaymentMethod());
            ps.setBigDecimal(6, o.getSubtotal());
            ps.setBigDecimal(7, o.getDiscountAmount());
            ps.setBigDecimal(8, o.getDeliveryCharge());
            ps.setBigDecimal(9, o.getTotalAmount());
            ps.setString(10, o.getShipContactName());
            ps.setString(11, o.getShipContactMobile());
            ps.setString(12, o.getShipLine1());
            ps.setString(13, o.getShipLine2());
            ps.setString(14, o.getShipCity());
            ps.setString(15, o.getShipState());
            ps.setString(16, o.getShipPincode());
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key != null ? key.longValue() : null;
    }

    @Override
    public void saveItems(Long orderId, List<OrderItem> items) {
        String sql = "INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity, line_total) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        for (OrderItem it : items) {
            jdbc.update(sql, orderId, it.getProductId(), it.getProductName(),
                    it.getUnitPrice(), it.getQuantity(), it.getLineTotal());
        }
    }

    @Override
    public Optional<Order> findById(Long id) {
        List<Order> list = jdbc.query("SELECT * FROM orders WHERE id = ?", ORDER_MAPPER, id);
        return list.stream().findFirst();
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        List<Order> list = jdbc.query("SELECT * FROM orders WHERE order_number = ?", ORDER_MAPPER, orderNumber);
        return list.stream().findFirst();
    }

    @Override
    public List<OrderItem> findItems(Long orderId) {
        return jdbc.query("SELECT * FROM order_items WHERE order_id = ? ORDER BY id", ITEM_MAPPER, orderId);
    }

    @Override
    public List<Order> findByMobile(String mobile) {
        return jdbc.query("SELECT * FROM orders WHERE user_mobile = ? ORDER BY id DESC", ORDER_MAPPER, mobile);
    }

    @Override
    public int updateStatuses(Long orderId, String status, String paymentStatus) {
        return jdbc.update("UPDATE orders SET status = ?, payment_status = ? WHERE id = ?",
                status, paymentStatus, orderId);
    }

    @Override
    public long count() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        return c != null ? c : 0;
    }
}
