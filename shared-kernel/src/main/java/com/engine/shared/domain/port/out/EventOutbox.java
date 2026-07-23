package com.engine.shared.domain.port.out;

import com.engine.shared.domain.event.DomainEvent;

/**
 * Driven port: persists {@link DomainEvent}s to the transactional outbox table
 * <strong>in the same database transaction</strong> that committed the aggregate state change.
 *
 * <p>This is the cornerstone of the Transactional Outbox Pattern. The application layer calls
 * {@link #append(DomainEvent)} for every event raised by an aggregate after persisting the
 * aggregate itself, but <em>before</em> the transaction commits. A separate asynchronous poller
 * (not visible at this port level) then drains outbox rows to the message broker.
 *
 * <p><strong>Why not publish directly to RabbitMQ from the transaction?</strong> A DB commit
 * succeeds independently of a broker publish; if the app crashes between the two, the event is
 * lost forever. Writing to the outbox in the same transaction guarantees that state and event
 * either both commit or both roll back &mdash; at-least-once delivery with idempotent consumers
 * gives us effectively-once semantics.
 *
 * <p>Implementations live in each bounded context's {@code adapters.out} package (one outbox
 * table per context, or a shared table with a context discriminator column).
 */
public interface EventOutbox {

    void append(DomainEvent event);
}