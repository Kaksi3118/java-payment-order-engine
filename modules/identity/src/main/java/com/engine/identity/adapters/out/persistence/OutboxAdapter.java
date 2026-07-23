package com.engine.identity.adapters.out.persistence;

import com.engine.shared.domain.event.DomainEvent;
import com.engine.shared.domain.port.out.EventOutbox;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Adapter: implements the {@link EventOutbox} driven port via a JPA outbox table.
 *
 * <p>Each call to {@link #append} serializes the {@link DomainEvent} to JSON and persists an
 * {@link OutboxEntity} row with {@link OutboxStatus#PENDING}. This MUST be called within the
 * same transaction as the aggregate state change so that the outbox row and the business state
 * commit atomically &mdash; the cornerstone of the Transactional Outbox Pattern.
 *
 * <p>Event serialization uses Jackson's {@link ObjectMapper}. The {@code event_type} column
 * stores the fully-qualified class name so the outbox poller (Stage 6) can deserialize the
 * payload back into the concrete event record.
 */
@Component
public class OutboxAdapter implements EventOutbox {

    private final OutboxJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxAdapter(OutboxJpaRepository jpaRepository, ObjectMapper objectMapper, Clock clock) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository, "OutboxJpaRepository must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
    }

    @Override
    public void append(DomainEvent event) {
        Objects.requireNonNull(event, "DomainEvent must not be null");

        String payload = serialize(event);
        OutboxEntity entity = new OutboxEntity(
                event.eventId(),
                event.aggregateId(),
                event.getClass().getName(),
                payload,
                OutboxStatus.PENDING,
                event.occurredAt(),
                Instant.now(clock));

        jpaRepository.save(entity);
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize domain event: " + event, e);
        }
    }
}