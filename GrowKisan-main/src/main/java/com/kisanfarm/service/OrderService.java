package com.kisanfarm.service;

import com.kisanfarm.dao.OrderDao;
import com.kisanfarm.dao.PaymentDao;
import com.kisanfarm.dao.ProductDao;
import com.kisanfarm.model.Order;
import com.kisanfarm.model.OrderItem;
import com.kisanfarm.model.Payment;
import com.kisanfarm.model.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Checkout orchestration. The backend is the single source of truth for all
 * prices: it looks up each product, uses its effective price (offer > discount
 * > MRP), computes the subtotal, delivery charge and total. The amount the
 * frontend sends is ignored.
 */
@Service
public class OrderService {

    private final ProductDao productDao;
    private final OrderDao orderDao;
    private final PaymentDao paymentDao;
    private final PaymentProvider paymentProvider;

    @Value("${kisanfarm.order.delivery-charge:49.00}")
    private BigDecimal deliveryCharge;

    @Value("${kisanfarm.order.free-delivery-threshold:499.00}")
    private BigDecimal freeDeliveryThreshold;

    public OrderService(ProductDao productDao, OrderDao orderDao, PaymentDao paymentDao,
                        PaymentProvider paymentProvider) {
        this.productDao = productDao;
        this.orderDao = orderDao;
        this.paymentDao = paymentDao;
        this.paymentProvider = paymentProvider;
    }

    public static class CheckoutException extends RuntimeException {
        public CheckoutException(String message) { super(message); }
    }

    /** A requested cart line from the frontend: productId + quantity only. */
    public record CartLine(Long productId, int quantity) {}

    /** Delivery/contact details captured before payment. */
    public record Address(String name, String mobile, String line1, String line2,
                          String city, String state, String pincode) {}

    @Transactional
    public CheckoutResult checkout(List<CartLine> lines, Address address) {
        if (lines == null || lines.isEmpty()) throw new CheckoutException("Your cart is empty.");
        validateAddress(address);

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (CartLine line : lines) {
            if (line.quantity() <= 0) continue;
            Product p = productDao.findById(line.productId())
                    .orElseThrow(() -> new CheckoutException("A product in your cart no longer exists."));

            if (!p.isInStock()) {
                throw new CheckoutException("\"" + p.getName() + "\" is out of stock.");
            }
            if (p.getStockQuantity() != null && line.quantity() > p.getStockQuantity()) {
                throw new CheckoutException("Only " + p.getStockQuantity() + " of \"" + p.getName() + "\" available.");
            }

            BigDecimal unit = p.getEffectivePrice();           // backend-authoritative price
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(line.quantity()));
            subtotal = subtotal.add(lineTotal);

            OrderItem oi = new OrderItem();
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setUnitPrice(unit);
            oi.setQuantity(line.quantity());
            oi.setLineTotal(lineTotal);
            items.add(oi);
        }

        if (items.isEmpty()) throw new CheckoutException("Your cart is empty.");

        BigDecimal delivery = subtotal.compareTo(freeDeliveryThreshold) >= 0 ? BigDecimal.ZERO : deliveryCharge;
        BigDecimal total = subtotal.add(delivery);

        // Create order (PENDING / payment PENDING).
        Order order = new Order();
        order.setOrderNumber(nextOrderNumber());
        order.setUserMobile(address.mobile());
        order.setStatus("PENDING");
        order.setPaymentStatus("PENDING");
        order.setPaymentMethod("ONLINE");
        order.setSubtotal(subtotal);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setDeliveryCharge(delivery);
        order.setTotalAmount(total);
        order.setShipContactName(address.name());
        order.setShipContactMobile(address.mobile());
        order.setShipLine1(address.line1());
        order.setShipLine2(address.line2());
        order.setShipCity(address.city());
        order.setShipState(address.state());
        order.setShipPincode(address.pincode());

        Long orderId = orderDao.save(order);
        order.setId(orderId);
        orderDao.saveItems(orderId, items);

        // Create payment intent for the backend-computed total.
        PaymentProvider.CreateResult pr = paymentProvider.createPayment(order.getOrderNumber(), total, "INR");

        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setProvider(paymentProvider.name());
        payment.setProviderPaymentId(pr.providerPaymentId());
        payment.setStatus("PENDING");
        payment.setAmount(total);
        payment.setCurrency("INR");
        payment.setClientSecret(pr.clientSecret());
        paymentDao.save(payment);

        order.setItems(items);
        return new CheckoutResult(order, pr.providerPaymentId(), pr.clientSecret(), paymentProvider.name());
    }

    /** Confirm a payment (verifies with the provider) and finalize the order. */
    @Transactional
    public Order confirmPayment(String orderNumber, String providerPaymentId) {
        Order order = orderDao.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new CheckoutException("Order not found."));
        Payment payment = paymentDao.findByOrderId(order.getId())
                .orElseThrow(() -> new CheckoutException("Payment not found."));

        // Idempotent: if already confirmed, just return.
        if ("SUCCESS".equals(payment.getStatus()) && "CONFIRMED".equals(order.getStatus())) {
            order.setItems(orderDao.findItems(order.getId()));
            return order;
        }

        boolean ok = paymentProvider.verifyPayment(
                providerPaymentId != null ? providerPaymentId : payment.getProviderPaymentId());

        if (!ok) {
            paymentDao.updateStatus(payment.getId(), "FAILED", providerPaymentId, "Verification failed");
            orderDao.updateStatuses(order.getId(), "PENDING", "FAILED");
            throw new CheckoutException("Payment could not be verified.");
        }

        paymentDao.updateStatus(payment.getId(), "SUCCESS", providerPaymentId, null);
        orderDao.updateStatuses(order.getId(), "CONFIRMED", "SUCCESS");

        // Decrement stock for each item.
        for (OrderItem it : orderDao.findItems(order.getId())) {
            productDao.findById(it.getProductId()).ifPresent(p -> {
                int remaining = Math.max(0, (p.getStockQuantity() != null ? p.getStockQuantity() : 0) - it.getQuantity());
                p.setStockQuantity(remaining);
                if (remaining == 0) p.setStatus("OUT_OF_STOCK");
                productDao.update(p);
            });
        }

        order.setStatus("CONFIRMED");
        order.setPaymentStatus("SUCCESS");
        order.setItems(orderDao.findItems(order.getId()));
        return order;
    }

    public Order getByNumber(String orderNumber) {
        Order o = orderDao.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new CheckoutException("Order not found."));
        o.setItems(orderDao.findItems(o.getId()));
        return o;
    }

    public List<Order> getOrdersForMobile(String mobile) {
        List<Order> orders = orderDao.findByMobile(mobile);
        for (Order o : orders) o.setItems(orderDao.findItems(o.getId()));
        return orders;
    }

    private void validateAddress(Address a) {
        if (a == null) throw new CheckoutException("Delivery address is required.");
        if (isBlank(a.name())) throw new CheckoutException("Contact name is required.");
        if (isBlank(a.mobile()) || a.mobile().replaceAll("\\D", "").length() < 10)
            throw new CheckoutException("A valid mobile number is required.");
        if (isBlank(a.line1())) throw new CheckoutException("Address line is required.");
        if (isBlank(a.city())) throw new CheckoutException("City is required.");
        if (isBlank(a.state())) throw new CheckoutException("State is required.");
        if (isBlank(a.pincode()) || a.pincode().replaceAll("\\D", "").length() != 6)
            throw new CheckoutException("A valid 6-digit pincode is required.");
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    /** Order numbers like GK10001, GK10002, ... */
    private String nextOrderNumber() {
        long n = 10001 + orderDao.count();
        return "GK" + n;
    }

    public record CheckoutResult(Order order, String providerPaymentId, String clientSecret, String provider) {}
}
