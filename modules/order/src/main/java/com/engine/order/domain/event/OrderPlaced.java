package com.engine.order.domain.event;

import com.engine.shared.domain.event.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Raised by {@link com.engine.order.domain.model.Order#place} when a new order is created.
 *
 * <p>Published to RabbitMQ so the Payment context can subscribe and initiate payment
 * authorization, and so any read-model projection (e.g. order history) can be updated.
 */
public record OrderPlaced(UUID eventId, Instant occurredAt, UUID aggregateId,
                          UUID customerId, List<LineItemSnapshot> items) implements DomainEvent {

    public OrderPlaced {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(items, "items must not be null");
        items = List.copyOf(items);
    }

    public record LineItemSnapshot(UUID productId, int quantity, java.math.BigDecimal unitPrice,
                                   String currency) {

        public LineItemSnapshot {
            Objects.requireNonNull(productId, "productId must not be null");
            Objects.requireNonNull(unitPrice, "unitPrice must not be null");
            Objects.requireNonNull(currency, "currency must not be null");
        }
    }

    public static List<LineItemSnapshot> toSnapshot(
            List<com.engine.order.domain.model.OrderItem> items) {
        return items.stream()
                .map(i -> new LineItemSnapshot(
                        i.productId(), i.quantity(),
                        i.unitPrice().amount(), i.unitPrice().currency().getCurrencyCode()))
                .toList();
    }
}