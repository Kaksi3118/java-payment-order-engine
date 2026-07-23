package com.engine.shared.domain.ids;

import com.engine.shared.domain.model.Identifier;

import java.util.Objects;
import java.util.UUID;

/**
 * Typed identifier for the User aggregate (Identity bounded context).
 */
public record UserId(UUID value) implements Identifier {

    public UserId {
        Objects.requireNonNull(value, "UserId value must not be null");
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId of(String literal) {
        Objects.requireNonNull(literal, "UserId literal must not be null");
        return new UserId(UUID.fromString(literal));
    }

    public static UserId random() {
        return new UserId(UUID.randomUUID());
    }
}