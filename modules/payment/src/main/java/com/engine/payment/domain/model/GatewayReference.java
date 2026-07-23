package com.engine.payment.domain.model;

import java.util.Objects;

public record GatewayReference(String value) {
    public GatewayReference {
        Objects.requireNonNull(value, "Gateway reference must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Gateway reference must not be blank");
        }
    }
}
