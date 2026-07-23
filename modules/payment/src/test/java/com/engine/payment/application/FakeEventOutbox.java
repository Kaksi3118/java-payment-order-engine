package com.engine.payment.application;

import com.engine.shared.domain.event.DomainEvent;
import com.engine.shared.domain.port.out.EventOutbox;

import java.util.ArrayList;
import java.util.List;

class FakeEventOutbox implements EventOutbox {

    private final List<DomainEvent> events = new ArrayList<>();

    @Override
    public void append(DomainEvent event) {
        events.add(event);
    }

    public List<DomainEvent> getEvents() {
        return events;
    }

    public void clear() {
        events.clear();
    }
}
