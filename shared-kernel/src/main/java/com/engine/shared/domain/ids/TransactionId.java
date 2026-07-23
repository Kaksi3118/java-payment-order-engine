package com.engine.shared.domain.ids;

import com.engine.shared.domain.model.Identifier;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for a single payment-processor transaction (Payment bounded
 * context). A {@link PaymentId} represents the aggregate, while a
 * {@code TransactionId} represents one interaction with the gateway
 * (authorization, capture, refund, void).
 */
public record TransactionId(UUID value) implements Identifier {

    public TransactionId {
        Objects.requireNonNull(value, "TransactionId value must not be null");
    }

    public static TransactionId of(UUID value) {
        return new TransactionId(value);
    }

    public static TransactionId of(String literal) {
        Objects.requireNonNull(literal, "TransactionId literal must not be null");
        return new TransactionId(UUID.fromString(literal));
    }

    public static TransactionId random() {
        return new TransactionId(UUID.randomUUID());
    }
}