package com.engine.identity.domain.event;

import com.engine.identity.domain.model.Email;
import com.engine.identity.domain.model.Roles;
import com.engine.shared.domain.event.DomainEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Raised by {@link com.engine.identity.domain.model.User#register} when a new user is created.
 *
 * <p>Published to RabbitMQ so any context that maintains a projection keyed by user (e.g. the
 * Order context's &quot;customer view&quot;) can subscribe and build its own read model without
 * querying the Identity context's database.
 */
public record UserRegistered(UUID eventId, Instant occurredAt, UUID aggregateId,
                             Email email, Roles roles) implements DomainEvent {

    public UserRegistered {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(roles, "roles must not be null");
    }
}