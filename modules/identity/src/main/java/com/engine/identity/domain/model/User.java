package com.engine.identity.domain.model;

import com.engine.identity.domain.event.UserActivated;
import com.engine.identity.domain.event.UserRegistered;
import com.engine.shared.domain.event.DomainEvent;
import com.engine.shared.domain.model.AggregateRoot;
import com.engine.shared.domain.ids.UserId;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Identity aggregate root.
 *
 * <p>The boundary of consistency for one user's authentication profile: their email, password
 * hash, roles, and lifecycle status. State transitions happen <em>only</em> through methods on
 * this aggregate &mdash; never via setters &mdash; and each transition that another context cares
 * about raises a {@link DomainEvent} collected by {@link AggregateRoot} and drained to the
 * transactional outbox by the application layer.
 *
 * <p><strong>Invariants enforced:</strong>
 * <ul>
 *     <li>Status transitions only through the legal lifecycle (see {@link UserStatus}).</li>
 *     <li>Cannot suspend / deactivate a non-ACTIVE user.</li>
 *     <li>Cannot activate an already-ACTIVE or terminal DEACTIVATED user.</li>
 * </ul>
 */
public final class User extends AggregateRoot {

    private final UserId id;
    private final Email email;
    private PasswordHash passwordHash;
    private Roles roles;
    private UserStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private User(UserId id, Email email, PasswordHash passwordHash, Roles roles,
                 UserStatus status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "UserId must not be null");
        this.email = Objects.requireNonNull(email, "Email must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "PasswordHash must not be null");
        this.roles = Objects.requireNonNull(roles, "Roles must not be null");
        this.status = Objects.requireNonNull(status, "UserStatus must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * Factory: a freshly registered user. Status begins at {@link UserStatus#PENDING}; an
     * explicit {@link #activate(Clock)} transition (typically triggered by email verification
     * or an admin) is required before authentication can succeed.
     */
    public static User register(Email email, PasswordHash passwordHash, Roles roles, Clock clock) {
        Instant now = Instant.now(Objects.requireNonNull(clock, "Clock must not be null"));
        User user = new User(UserId.random(), email, passwordHash, roles, UserStatus.PENDING, now, now);
        user.raise(new UserRegistered(user.id.value(), now, user.id.value(), user.email, user.roles));
        return user;
    }

    /**
     * Reconstitution factory: reconstructs a {@link User} from persisted state <strong>without
     * raising domain events</strong>. Used exclusively by the persistence adapter
     * ({@code UserRepositoryAdapter}) when loading from the database &mdash; events were already
     * raised and published when the user was originally created or transitioned.
     *
     * <p>This is the standard DDD reconstitution pattern: the aggregate is rebuilt from its
     * constituent parts exactly as it was persisted, with no side effects.
     */
    public static User reconstitute(UserId id, Email email, PasswordHash passwordHash, Roles roles,
                                    UserStatus status, Instant createdAt, Instant updatedAt) {
        return new User(id, email, passwordHash, roles, status, createdAt, updatedAt);
    }

    public void activate(Clock clock) {
        if (status == UserStatus.ACTIVE) {
            throw new IllegalStateException("User is already ACTIVE");
        }
        if (status == UserStatus.DEACTIVATED) {
            throw new IllegalStateException("Cannot activate a DEACTIVATED user (terminal state)");
        }
        status = UserStatus.ACTIVE;
        updatedAt = Instant.now(clock);
        raise(new UserActivated(id.value(), updatedAt, id.value()));
    }

    public void suspend(Clock clock) {
        if (status != UserStatus.ACTIVE) {
            throw new IllegalStateException("Only an ACTIVE user can be suspended; current status: " + status);
        }
        status = UserStatus.SUSPENDED;
        updatedAt = Instant.now(clock);
    }

    public void deactivate(Clock clock) {
        if (status == UserStatus.DEACTIVATED) {
            throw new IllegalStateException("User is already DEACTIVATED");
        }
        if (status != UserStatus.ACTIVE) {
            throw new IllegalStateException("Only an ACTIVE user can be deactivated; current status: " + status);
        }
        status = UserStatus.DEACTIVATED;
        updatedAt = Instant.now(clock);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public UserId id() {
        return id;
    }

    public UUID idValue() {
        return id.value();
    }

    public Email email() {
        return email;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public Roles roles() {
        return roles;
    }

    public UserStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}