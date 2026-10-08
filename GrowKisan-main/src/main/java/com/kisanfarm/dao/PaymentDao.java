package com.kisanfarm.dao;

import com.kisanfarm.model.Payment;

import java.util.Optional;

public interface PaymentDao {
    Long save(Payment payment);
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByProviderPaymentId(String providerPaymentId);
    int updateStatus(Long id, String status, String providerPaymentId, String failureReason);
}
