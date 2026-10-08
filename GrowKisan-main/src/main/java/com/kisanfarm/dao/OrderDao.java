package com.kisanfarm.dao;

import com.kisanfarm.model.Order;
import com.kisanfarm.model.OrderItem;

import java.util.List;
import java.util.Optional;

public interface OrderDao {
    Long save(Order order);
    void saveItems(Long orderId, List<OrderItem> items);
    Optional<Order> findById(Long id);
    Optional<Order> findByOrderNumber(String orderNumber);
    List<OrderItem> findItems(Long orderId);
    List<Order> findByMobile(String mobile);
    int updateStatuses(Long orderId, String status, String paymentStatus);
    long count();
}
