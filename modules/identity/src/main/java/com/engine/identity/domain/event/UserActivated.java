package com.engine.identity.domain.event;

import com.engine.shared.domain.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Raised by {@link com.engine.identity.domain.model.User#activate} when a user transitions into
 * the {@link com.engine.identity.domain.model.UserStatus#ACTIVE} state &mdash; the moment they
 * become eligible to authenticate.
 */
public record UserActivated(UUID eventId, Instant occurredAt, UUID aggregateId) implements DomainEvent {

    public UserActivated {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
    }
}