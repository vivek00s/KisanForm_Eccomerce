package com.kisanfarm.controller;

import com.kisanfarm.model.Order;
import com.kisanfarm.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Checkout + order endpoints.
 *   POST /api/checkout            { items:[{productId,quantity}], address:{...} }
 *                                 -> creates order + payment intent, returns clientSecret
 *   POST /api/checkout/confirm    { orderNumber, providerPaymentId } -> confirms & finalizes
 *   GET  /api/orders/{orderNumber}
 *   GET  /api/orders?mobile=...
 */
@RestController
@RequestMapping("/api")
public class CheckoutController {

    private final OrderService orderService;

    public CheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@RequestBody Map<String, Object> body) {
        List<OrderService.CartLine> lines = parseLines(body.get("items"));
        OrderService.Address address = parseAddress(body.get("address"));

        OrderService.CheckoutResult result = orderService.checkout(lines, address);

        Map<String, Object> resp = new HashMap<>();
        resp.put("orderNumber", result.order().getOrderNumber());
        resp.put("amount", result.order().getTotalAmount());
        resp.put("provider", result.provider());
        resp.put("providerPaymentId", result.providerPaymentId());
        resp.put("clientSecret", result.clientSecret());
        resp.put("order", toOrderMap(result.order()));
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/checkout/confirm")
    public ResponseEntity<Map<String, Object>> confirm(@RequestBody Map<String, String> body) {
        Order order = orderService.confirmPayment(body.get("orderNumber"), body.get("providerPaymentId"));
        return ResponseEntity.ok(toOrderMap(order));
    }

    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(toOrderMap(orderService.getByNumber(orderNumber)));
    }

    @GetMapping("/orders")
    public List<Map<String, Object>> myOrders(@RequestParam String mobile) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Order o : orderService.getOrdersForMobile(mobile)) out.add(toOrderMap(o));
        return out;
    }

    @ExceptionHandler(OrderService.CheckoutException.class)
    public ResponseEntity<Map<String, String>> handle(OrderService.CheckoutException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    // ---------------- parsing / mapping ----------------

    @SuppressWarnings("unchecked")
    private List<OrderService.CartLine> parseLines(Object raw) {
        List<OrderService.CartLine> lines = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    Long pid = Long.valueOf(String.valueOf(m.get("productId")));
                    int qty = Integer.parseInt(String.valueOf(m.get("quantity")));
                    lines.add(new OrderService.CartLine(pid, qty));
                }
            }
        }
        return lines;
    }

    @SuppressWarnings("unchecked")
    private OrderService.Address parseAddress(Object raw) {
        Map<String, Object> m = raw instanceof Map ? (Map<String, Object>) raw : Map.of();
        return new OrderService.Address(
                str(m.get("name")), str(m.get("mobile")), str(m.get("line1")), str(m.get("line2")),
                str(m.get("city")), str(m.get("state")), str(m.get("pincode")));
    }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }

    private Map<String, Object> toOrderMap(Order o) {
        Map<String, Object> m = new HashMap<>();
        m.put("orderNumber", o.getOrderNumber());
        m.put("status", o.getStatus());
        m.put("paymentStatus", o.getPaymentStatus());
        m.put("subtotal", o.getSubtotal());
        m.put("discountAmount", o.getDiscountAmount());
        m.put("deliveryCharge", o.getDeliveryCharge());
        m.put("totalAmount", o.getTotalAmount());
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        Map<String, Object> addr = new HashMap<>();
        addr.put("name", o.getShipContactName());
        addr.put("mobile", o.getShipContactMobile());
        addr.put("line1", o.getShipLine1());
        addr.put("line2", o.getShipLine2());
        addr.put("city", o.getShipCity());
        addr.put("state", o.getShipState());
        addr.put("pincode", o.getShipPincode());
        m.put("address", addr);
        List<Map<String, Object>> items = new ArrayList<>();
        if (o.getItems() != null) {
            o.getItems().forEach(it -> {
                Map<String, Object> im = new HashMap<>();
                im.put("productId", it.getProductId());
                im.put("productName", it.getProductName());
                im.put("unitPrice", it.getUnitPrice());
                im.put("quantity", it.getQuantity());
                im.put("lineTotal", it.getLineTotal());
                items.add(im);
            });
        }
        m.put("items", items);
        return m;
    }
}
