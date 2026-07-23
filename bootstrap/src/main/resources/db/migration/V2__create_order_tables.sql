-- V2__create_order_tables.sql
-- Order bounded context tables: orders, order_items, idempotency_keys.

-- ========== orders ==========
CREATE TABLE orders (
    id              UUID          PRIMARY KEY,
    customer_id     UUID          NOT NULL,
    order_currency  VARCHAR(3)    NOT NULL,
    status          VARCHAR(50)   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0
);

-- ========== order_items ==========
CREATE TABLE order_items (
    order_id    UUID           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  UUID           NOT NULL,
    quantity    INTEGER        NOT NULL,
    unit_price  NUMERIC(19, 4) NOT NULL,
    currency    VARCHAR(3)     NOT NULL
);

-- ========== idempotency_keys ==========
CREATE TABLE idempotency_keys (
    idempotency_key  VARCHAR(255)  PRIMARY KEY,
    request_hash     VARCHAR(255)  NOT NULL,
    result_json      TEXT          NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL
);