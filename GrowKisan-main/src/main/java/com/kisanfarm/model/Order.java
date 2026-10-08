package com.kisanfarm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Order header (no JPA). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private Long id;
    private String orderNumber;      // e.g. GK10025
    private String userMobile;
    private String status;           // PENDING|CONFIRMED|PROCESSING|SHIPPED|DELIVERED|CANCELLED
    private String paymentStatus;    // PENDING|SUCCESS|FAILED|REFUNDED
    private String paymentMethod;    // ONLINE|COD

    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal deliveryCharge;
    private BigDecimal totalAmount;

    // Delivery address snapshot
    private String shipContactName;
    private String shipContactMobile;
    private String shipLine1;
    private String shipLine2;
    private String shipCity;
    private String shipState;
    private String shipPincode;

    private Instant createdAt;
    private Instant updatedAt;

    private List<OrderItem> items = new ArrayList<>();
}
