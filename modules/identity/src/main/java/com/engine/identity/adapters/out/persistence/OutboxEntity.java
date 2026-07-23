package com.engine.identity.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for the {@code outbox_events} table &mdash; the transactional outbox.
 *
 * <p>Each row represents one {@link com.engine.shared.domain.event.DomainEvent} that was raised
 * by an aggregate and written in the same DB transaction as the aggregate state change. An
 * asynchronous poller (implemented in Stage 6) leases PENDING rows, publishes their payload to
 * RabbitMQ, and marks them PUBLISHED.
 *
 * <p>The {@code payload} column stores the JSON-serialized event. The {@code event_type} column
 * stores the fully-qualified Java class name so the poller can deserialize the event back into
 * its concrete record type before publishing.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected OutboxEntity() {
    }

    public OutboxEntity(UUID id, UUID aggregateId, String eventType, String payload,
                        OutboxStatus status, Instant occurredAt, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.retryCount = 0;
    }

    public UUID getId() { return id; }
    public UUID getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public OutboxStatus getStatus() { return status; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public int getRetryCount() { return retryCount; }
    public Long getVersion() { return version; }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
    }
}