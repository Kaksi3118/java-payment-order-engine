package com.engine.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Contract for every domain event raised by an aggregate.
 *
 * <p>Concrete events are <strong>records</strong> that implement this interface
 * and declare the three metadata components plus their payload, e.g.
 * <pre>{@code
 * public record OrderPlaced(UUID eventId, Instant occurredAt, UUID aggregateId,
 *                          OrderId placedBy, Money total) implements DomainEvent {}
 * }</pre>
 *
 * <p>{@link #aggregateId()} returns a raw {@link UUID} (not a typed identifier)
 * deliberately: events cross bounded-context boundaries and must stay decoupled
 * from any single context's identifier types so an inbound consumer does not have
 * to depend on the publishing context's kernel.
 */
public interface DomainEvent {
    UUID eventId();
    Instant occurredAt();
    UUID aggregateId();
}