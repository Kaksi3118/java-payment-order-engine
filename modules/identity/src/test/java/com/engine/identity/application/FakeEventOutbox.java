package com.engine.identity.application;

import com.engine.shared.domain.event.DomainEvent;
import com.engine.shared.domain.port.out.EventOutbox;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake {@link EventOutbox} for unit tests &mdash; collects events in a list so the test
 * can assert exactly which domain events were drained from the aggregate to the outbox.
 */
final class FakeEventOutbox implements EventOutbox {

    private final List<DomainEvent> events = new ArrayList<>();

    @Override
    public void append(DomainEvent event) {
        events.add(event);
    }

    List<DomainEvent> events() {
        return List.copyOf(events);
    }

    int size() {
        return events.size();
    }
}