-- KisanFarm schema (JDBC). Grows as more features are added.

CREATE TABLE IF NOT EXISTS products (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku              VARCHAR(60)  NOT NULL,
    name             VARCHAR(200) NOT NULL,
    description      TEXT,
    category_id      BIGINT,
    brand            VARCHAR(120),
    image_url        VARCHAR(500),
    price            DECIMAL(12,2) NOT NULL,
    discount_percent DECIMAL(5,2)  NOT NULL DEFAULT 0,
    rating           DECIMAL(3,2)  DEFAULT 0,
    review_count     INT           NOT NULL DEFAULT 0,
    badge            VARCHAR(30),
    status           VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    stock_quantity   INT           NOT NULL DEFAULT 0,
    offer_active     TINYINT(1)    NOT NULL DEFAULT 0,
    offer_label      VARCHAR(60),
    offer_price      DECIMAL(12,2),
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_products_sku (sku),
    KEY idx_products_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Admin/seller accounts (username + password login, separate from buyer OTP).
CREATE TABLE IF NOT EXISTS admin_users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(60)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    name          VARCHAR(120),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_admin_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed a hardcoded admin: username = admin, password = admin123
-- password_hash is SHA-256 hex of "admin123".
INSERT INTO admin_users (username, password_hash, name)
SELECT 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'KisanFarm Admin'
WHERE NOT EXISTS (SELECT 1 FROM admin_users WHERE username = 'admin');

-- Column adds for pre-existing products tables (older DBs). These run every
-- startup; if a column already exists MySQL errors, which is ignored because
-- spring.sql.init.continue-on-error=true. Fresh DBs already have them above.
ALTER TABLE products ADD COLUMN image_url VARCHAR(500);
ALTER TABLE products ADD COLUMN stock_quantity INT NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN offer_active TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN offer_label VARCHAR(60);
ALTER TABLE products ADD COLUMN offer_price DECIMAL(12,2);

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    mobile      VARCHAR(20)  NOT NULL,
    name        VARCHAR(120),
    email       VARCHAR(160),
    role        VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    active      TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_mobile (mobile)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- OTP requests. We do NOT store the raw OTP when using Twilio Verify (Twilio
-- holds it). For the local mock provider we store a hash only, never plain text.
CREATE TABLE IF NOT EXISTS otp_requests (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    mobile        VARCHAR(20) NOT NULL,
    provider      VARCHAR(40) NOT NULL,
    provider_ref  VARCHAR(120),
    code_hash     VARCHAR(100),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts      INT         NOT NULL DEFAULT 0,
    expires_at    TIMESTAMP   NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_otp_mobile (mobile),
    KEY idx_otp_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===================== Cart / Checkout / Orders / Payments =====================

CREATE TABLE IF NOT EXISTS orders (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number        VARCHAR(20)  NOT NULL,
    user_mobile         VARCHAR(20),
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    payment_status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    payment_method      VARCHAR(20)  NOT NULL DEFAULT 'ONLINE',
    subtotal            DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_amount     DECIMAL(12,2) NOT NULL DEFAULT 0,
    delivery_charge     DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_amount        DECIMAL(12,2) NOT NULL DEFAULT 0,
    ship_contact_name   VARCHAR(120),
    ship_contact_mobile VARCHAR(20),
    ship_line1          VARCHAR(200),
    ship_line2          VARCHAR(200),
    ship_city           VARCHAR(100),
    ship_state          VARCHAR(100),
    ship_pincode        VARCHAR(10),
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_orders_number (order_number),
    KEY idx_orders_mobile (user_mobile),
    KEY idx_orders_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_items (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id      BIGINT NOT NULL,
    product_id    BIGINT NOT NULL,
    product_name  VARCHAR(200) NOT NULL,
    unit_price    DECIMAL(12,2) NOT NULL,
    quantity      INT NOT NULL,
    line_total    DECIMAL(12,2) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order_items_order (order_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payments (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id            BIGINT NOT NULL,
    provider            VARCHAR(40) NOT NULL,
    provider_payment_id VARCHAR(120),
    provider_order_id   VARCHAR(120),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount              DECIMAL(12,2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'INR',
    client_secret       VARCHAR(255),
    failure_reason      VARCHAR(300),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_payments_order (order_id),
    KEY idx_payments_status (status),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
