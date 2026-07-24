package com.engine.order.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity for the {@code idempotency_keys} table.
 *
 * <p>Stores the idempotency key, a hash of the request body, and the cached result JSON.
 * The UNIQUE constraint on {@code idempotency_key} ensures that a concurrent insert race
 * is resolved by the database &mdash; two requests cannot both save the same key.
 */
@Entity(name = "OrderIdempotency")
@Table(name = "idempotency_keys")
public class IdempotencyEntity {

    @Id
    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(name = "result_json", nullable = false, columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyEntity() {
    }

    public IdempotencyEntity(String idempotencyKey, String requestHash, String resultJson, Instant createdAt) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        this.requestHash = Objects.requireNonNull(requestHash, "requestHash must not be null");
        this.resultJson = Objects.requireNonNull(resultJson, "resultJson must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public String getResultJson() { return resultJson; }
    public Instant getCreatedAt() { return createdAt; }
}