package com.engine.order.application;

import com.engine.shared.domain.event.DomainEvent;
import com.engine.shared.domain.port.out.EventOutbox;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake {@link EventOutbox} for unit tests &mdash; collects events for assertions.
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

    void clear() {
        events.clear();
    }
}