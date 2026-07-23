package com.engine.shared.domain.model;

import com.engine.shared.domain.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AggregateRoot")
class AggregateRootTest {

    @Test
    @DisplayName("raises and exposes domain events until cleared")
    void collectsAndClearsEvents() {
        var aggregate = new TestAggregate();
        var first = new TestEvent();
        var second = new TestEvent();
        aggregate.raise(first);
        aggregate.raise(second);

        assertThat(aggregate.domainEvents()).containsExactly(first, second);

        aggregate.clearEvents();
        assertThat(aggregate.domainEvents()).isEmpty();
    }

    @Test
    @DisplayName("domainEvents returns an immutable snapshot")
    void domainEventsIsImmutableSnapshot() {
        var aggregate = new TestAggregate();
        aggregate.raise(new TestEvent());

        assertThatThrownBy(() -> aggregate.domainEvents().add(new TestEvent()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("raising a null event is rejected")
    void raisingNullEventIsRejected() {
        var aggregate = new TestAggregate();
        assertThatThrownBy(() -> aggregate.raise(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class TestAggregate extends AggregateRoot {
    }

    private static final class TestEvent implements DomainEvent {
        private final UUID eventId = UUID.randomUUID();
        private final Instant occurredAt = Instant.now();
        private final UUID aggregateId = UUID.randomUUID();

        @Override public UUID eventId() { return eventId; }
        @Override public Instant occurredAt() { return occurredAt; }
        @Override public UUID aggregateId() { return aggregateId; }
    }
}