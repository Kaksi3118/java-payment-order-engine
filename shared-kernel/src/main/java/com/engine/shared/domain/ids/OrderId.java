package com.engine.shared.domain.ids;

import com.engine.shared.domain.model.Identifier;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for the Order aggregate.
 */
public record OrderId(UUID value) implements Identifier {

    public OrderId {
        Objects.requireNonNull(value, "OrderId value must not be null");
    }

    public static OrderId of(UUID value) {
        return new OrderId(value);
    }

    public static OrderId of(String literal) {
        Objects.requireNonNull(literal, "OrderId literal must not be null");
        return new OrderId(UUID.fromString(literal));
    }

    public static OrderId random() {
        return new OrderId(UUID.randomUUID());
    }
}