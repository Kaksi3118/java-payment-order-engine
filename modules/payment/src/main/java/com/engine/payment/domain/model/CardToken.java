package com.engine.payment.domain.model;

import java.util.Objects;

public record CardToken(String value) {
    public CardToken {
        Objects.requireNonNull(value, "Card token must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Card token must not be blank");
        }
    }
}
