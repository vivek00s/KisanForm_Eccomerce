package com.kisanfarm.controller;

import com.kisanfarm.dao.ProductDao;
import com.kisanfarm.model.Product;
import com.kisanfarm.service.AdminTokenGuard;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Product API.
 *   Public (buyer):  GET /api/products, GET /api/products/{id}
 *   Admin (seller):  POST/PUT/DELETE and stock/offer updates. Guarded by the
 *                    X-Admin-Token header (issued by /api/admin/login).
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductDao productDao;
    private final AdminTokenGuard adminGuard;

    public ProductController(ProductDao productDao, AdminTokenGuard adminGuard) {
        this.productDao = productDao;
        this.adminGuard = adminGuard;
    }

    // ---------------- Public buyer endpoints ----------------

    @GetMapping
    public List<Product> list() {
        return productDao.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return productDao.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------------- Admin endpoints ----------------

    @PostMapping
    public ResponseEntity<Product> create(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                          @RequestBody Product product) {
        adminGuard.require(token);
        if (product.getStatus() == null) product.setStatus("ACTIVE");
        Long id = productDao.save(product);
        product.setId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                          @PathVariable Long id, @RequestBody Product product) {
        adminGuard.require(token);
        product.setId(id);
        int rows = productDao.update(product);
        return rows > 0 ? ResponseEntity.ok(product) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                       @PathVariable Long id) {
        adminGuard.require(token);
        int rows = productDao.deleteById(id);
        return rows > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /** Set absolute stock quantity for a product. */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> setStock(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminGuard.require(token);
        Product p = productDao.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        int qty = toInt(body.get("stockQuantity"));
        p.setStockQuantity(Math.max(0, qty));
        // Auto-flag status based on stock.
        p.setStatus(p.getStockQuantity() > 0 ? "ACTIVE" : "OUT_OF_STOCK");
        productDao.update(p);
        return ResponseEntity.ok(p);
    }

    /** Set or clear a festival offer on a product. */
    @PatchMapping("/{id}/offer")
    public ResponseEntity<Product> setOffer(@RequestHeader(value = "X-Admin-Token", required = false) String token,
                                            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        adminGuard.require(token);
        Product p = productDao.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();

        boolean active = Boolean.TRUE.equals(body.get("offerActive"))
                || "true".equalsIgnoreCase(String.valueOf(body.get("offerActive")));
        p.setOfferActive(active);
        p.setOfferLabel(active ? String.valueOf(body.getOrDefault("offerLabel", "SPECIAL OFFER")) : null);
        Object op = body.get("offerPrice");
        p.setOfferPrice(active && op != null ? new BigDecimal(String.valueOf(op)) : null);
        productDao.update(p);
        return ResponseEntity.ok(p);
    }

    @ExceptionHandler(AdminTokenGuard.ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(AdminTokenGuard.ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
    }

    private static int toInt(Object o) {
        if (o == null) return 0;
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
}
