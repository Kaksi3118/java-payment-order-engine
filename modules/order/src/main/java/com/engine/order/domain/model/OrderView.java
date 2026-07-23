package com.engine.order.domain.model;

import com.engine.shared.domain.model.Money;

import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Read model for the order query side (CQRS).
 *
 * <p>This is a snapshot of the order's state at query time &mdash; not a live
 * aggregate. The application layer's query handler constructs this from the
 * persisted order without loading the full {@link Order} aggregate or its event
 * buffer, keeping read paths fast and decoupled from write-side invariants.
 */
public record OrderView(
        java.util.UUID orderId,
        java.util.UUID customerId,
        OrderStatus status,
        Currency currency,
        List<OrderItem> items,
        Money totalAmount,
        java.time.Instant createdAt,
        java.time.Instant updatedAt
) {

    public OrderView {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(totalAmount, "totalAmount must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        items = List.copyOf(items);
    }
}