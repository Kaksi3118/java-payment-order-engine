package com.engine.order.domain.event;

import com.engine.shared.domain.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Raised by {@link com.engine.order.domain.model.Order#confirm} when an order transitions
 * from CREATED to CONFIRMED &mdash; inventory has been reserved and the order is ready
 * for payment processing.
 */
public record OrderConfirmed(UUID eventId, Instant occurredAt, UUID aggregateId) implements DomainEvent {

    public OrderConfirmed {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
    }
}