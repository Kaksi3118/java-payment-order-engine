package com.engine.order.domain.event;

import com.engine.shared.domain.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Raised by {@link com.engine.order.domain.model.Order#cancel} when an order is cancelled.
 * The Payment context subscribes to this event to release any held authorization.
 */
public record OrderCancelled(UUID eventId, Instant occurredAt, UUID aggregateId,
                             UUID customerId) implements DomainEvent {

    public OrderCancelled {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
    }
}