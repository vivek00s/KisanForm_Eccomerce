package com.kisanfarm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Plain product model (no JPA). Maps to the 'products' table and is populated
 * via JDBC Template RowMappers in the DAO implementation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private Long categoryId;
    private String brand;
    private String imageUrl;

    /** List price / MRP. */
    private BigDecimal price;

    /** Discount percentage (0-100). */
    private BigDecimal discountPercent;

    private BigDecimal rating;
    private Integer reviewCount;
    private String badge;

    /** ACTIVE | INACTIVE | OUT_OF_STOCK */
    private String status;

    // --- Inventory ---
    private Integer stockQuantity;

    // --- Festival / special offer ---
    private Boolean offerActive;
    private String offerLabel;      // e.g. "DIWALI 20% OFF"
    private BigDecimal offerPrice;  // special price during the offer

    /**
     * Price the buyer actually pays:
     *   - if an offer is active and has a price, use the offer price;
     *   - else apply discountPercent to the MRP;
     *   - else the MRP.
     * Backend remains the source of truth for all pricing.
     */
    public BigDecimal getEffectivePrice() {
        if (price == null) return BigDecimal.ZERO;
        if (Boolean.TRUE.equals(offerActive) && offerPrice != null && offerPrice.signum() > 0) {
            return offerPrice.setScale(2, RoundingMode.HALF_UP);
        }
        if (discountPercent != null && discountPercent.signum() > 0) {
            BigDecimal disc = price.multiply(discountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return price.subtract(disc).setScale(2, RoundingMode.HALF_UP);
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }
}
