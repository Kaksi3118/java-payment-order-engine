CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    amount_value BIGINT NOT NULL,
    amount_currency VARCHAR(3) NOT NULL,
    captured_value BIGINT NOT NULL,
    captured_currency VARCHAR(3) NOT NULL,
    refunded_value BIGINT NOT NULL,
    refunded_currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    gateway_reference VARCHAR(255),
    version BIGINT NOT NULL
);

CREATE TABLE payment_idempotency_keys (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    request_hash VARCHAR(255) NOT NULL,
    result_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
