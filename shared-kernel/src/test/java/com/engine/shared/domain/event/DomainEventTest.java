package com.engine.shared.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DomainEvent contract")
class DomainEventTest {

    @Test
    @DisplayName("a concrete record implementing the interface exposes the three metadata fields")
    void sampleEventHonoursContract() {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        UUID aggregateId = UUID.randomUUID();

        DomainEvent event = new SampleEvent(eventId, occurredAt, aggregateId, "payload");

        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(event.occurredAt()).isEqualTo(occurredAt);
        assertThat(event.aggregateId()).isEqualTo(aggregateId);
        assertThat(event).isInstanceOf(DomainEvent.class);
    }

    private record SampleEvent(UUID eventId, Instant occurredAt, UUID aggregateId, String payload)
            implements DomainEvent {}
}