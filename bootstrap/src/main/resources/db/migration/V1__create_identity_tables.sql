-- V1__create_identity_tables.sql
-- First Flyway migration: Identity bounded context tables (users, user_roles, outbox_events).
-- Forward-only: never edit this file after it has shipped to any environment.

-- ========== users ==========
CREATE TABLE users (
    id              UUID          PRIMARY KEY,
    email           VARCHAR(255)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,
    status          VARCHAR(50)   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0
);

-- ========== user_roles ==========
CREATE TABLE user_roles (
    user_id  UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role     VARCHAR(50)  NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- ========== outbox_events ==========
-- Transactional Outbox Pattern: domain events written in-transaction, dispatched asynchronously.
CREATE TABLE outbox_events (
    id            UUID          PRIMARY KEY,
    aggregate_id  UUID          NOT NULL,
    event_type    VARCHAR(255)  NOT NULL,
    payload       TEXT          NOT NULL,
    status        VARCHAR(50)   NOT NULL,
    occurred_at   TIMESTAMPTZ   NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL,
    retry_count   INTEGER       NOT NULL DEFAULT 0,
    version       BIGINT        NOT NULL DEFAULT 0
);

-- Index for the outbox poller's lease query: SELECT ... WHERE status = 'PENDING' ORDER BY created_at
CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at);