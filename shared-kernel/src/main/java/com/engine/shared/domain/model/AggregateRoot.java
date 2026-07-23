package com.engine.shared.domain.model;

import com.engine.shared.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for every DDD aggregate root.
 *
 * <p>An aggregate is a transactional consistency boundary. The only thing this
 * base class does is collect the {@link DomainEvent}s raised during the current
 * transaction so the application layer can persist them to the outbox table in
 * the <em>same</em> database transaction that committed the state change. Once the
 * outbox rows are persisted, {@link #clearEvents()} is called to reset the buffer;
 * the asynchronous poller then drains the outbox to the message broker.
 *
 * <p><strong>Invariant:</strong> subclasses MUST call {@link #raise(DomainEvent)}
 * only from within a state-changing method, never as a side read.
 */
public abstract class AggregateRoot {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected final void raise(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Domain event must not be null");
        }
        domainEvents.add(event);
    }

    /**
     * Returns an immutable snapshot of the events raised since the last
     * {@link #clearEvents()}. The application layer reads this to append the
     * corresponding rows to the outbox, then calls {@link #clearEvents()}.
     */
    public final List<DomainEvent> domainEvents() {
        return List.copyOf(domainEvents);
    }

    /**
     * Clears the internal event buffer. Must only be invoked after the matching
     * outbox rows have been persisted in the same database transaction.
     */
    public final void clearEvents() {
        domainEvents.clear();
    }
}