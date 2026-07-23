package com.engine.shared.domain.ids;

import com.engine.shared.domain.model.Identifier;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for the Payment aggregate.
 */
public record PaymentId(UUID value) implements Identifier {

    public PaymentId {
        Objects.requireNonNull(value, "PaymentId value must not be null");
    }

    public static PaymentId of(UUID value) {
        return new PaymentId(value);
    }

    public static PaymentId of(String literal) {
        Objects.requireNonNull(literal, "PaymentId literal must not be null");
        return new PaymentId(UUID.fromString(literal));
    }

    public static PaymentId random() {
        return new PaymentId(UUID.randomUUID());
    }
}