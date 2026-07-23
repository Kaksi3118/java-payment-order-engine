package com.engine.order.domain.model;

import com.engine.shared.domain.model.Money;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable line item within an {@link Order}.
 *
 * <p>Each item references a product by its UUID, carries an ordered quantity,
 * and a unit price as {@link Money}. The {@link Order} aggregate enforces that
 * all items in one order share the same currency &mdash; mixed-currency orders
 * are rejected at construction time.
 */
public record OrderItem(UUID productId, int quantity, Money unitPrice) {

    public OrderItem {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive; was: " + quantity);
        }
    }

    /**
     * Line total = unit price &times; quantity.
     */
    public Money lineTotal() {
        return unitPrice.multiplyBy(quantity);
    }
}