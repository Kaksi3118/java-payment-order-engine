package com.engine.identity.domain.model;

import com.engine.identity.domain.event.UserActivated;
import com.engine.identity.domain.event.UserRegistered;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("User aggregate")
class UserTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC);

    private static User newUserPending() {
        return User.register(
                Email.of("alice@example.com"),
                PasswordHash.of("$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMN"),
                Roles.of(Role.CUSTOMER),
                FIXED_CLOCK);
    }

    @Nested
    @DisplayName("register()")
    class RegisterFactory {

        @Test
        @DisplayName("starts in PENDING and records createdAt == updatedAt")
        void startsPending() {
            User user = User.register(
                    Email.of("alice@example.com"),
                    PasswordHash.of("$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMN"),
                    Roles.of(Role.CUSTOMER),
                    FIXED_CLOCK);

            assertThat(user.status()).isEqualTo(UserStatus.PENDING);
            assertThat(user.createdAt()).isEqualTo(Instant.parse("2026-07-23T12:00:00Z"));
            assertThat(user.updatedAt()).isEqualTo(user.createdAt());
            assertThat(user.isActive()).isFalse();
        }

        @Test
        @DisplayName("raises a UserRegistered event carrying email and roles")
        void raisesRegisteredEvent() {
            User user = newUserPending();

            assertThat(user.domainEvents())
                    .singleElement()
                    .isInstanceOf(UserRegistered.class);

            UserRegistered event = (UserRegistered) user.domainEvents().get(0);
            assertThat(event.aggregateId()).isEqualTo(user.idValue());
            assertThat(event.email()).isEqualTo(Email.of("alice@example.com"));
            assertThat(event.roles()).isEqualTo(Roles.of(Role.CUSTOMER));
            assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-07-23T12:00:00Z"));
        }

        @Test
        @DisplayName("clearEvents empties the event buffer after the outbox drains it")
        void clearEvents() {
            User user = newUserPending();
            assertThat(user.domainEvents()).isNotEmpty();

            user.clearEvents();
            assertThat(user.domainEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("lifecycle transitions")
    class Transitions {

        @Test
        @DisplayName("activate: PENDING -> ACTIVE raises UserActivated")
        void pendingToActive() {
            User user = newUserPending();
            user.activate(FIXED_CLOCK);

            assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.isActive()).isTrue();

            UserActivated event = (UserActivated) user.domainEvents().get(1);
            assertThat(event.aggregateId()).isEqualTo(user.idValue());
        }

        @Test
        @DisplayName("activate: SUSPENDED -> ACTIVE is allowed")
        void suspendedToActive() {
            User user = newUserPending();
            user.activate(FIXED_CLOCK);
            user.suspend(FIXED_CLOCK);
            user.activate(FIXED_CLOCK);
            assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("activate: already-ACTIVE is rejected")
        void alreadyActiveRejected() {
            User user = newUserPending();
            user.activate(FIXED_CLOCK);
            assertThatThrownBy(() -> user.activate(FIXED_CLOCK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already ACTIVE");
        }

        @Test
        @DisplayName("activate: from terminal DEACTIVATED is rejected")
        void deactivatedCannotBeReactivated() {
            User user = newUserPending();
            user.activate(FIXED_CLOCK);
            user.deactivate(FIXED_CLOCK);
            assertThatThrownBy(() -> user.activate(FIXED_CLOCK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DEACTIVATED");
        }

        @Test
        @DisplayName("suspend: only ACTIVE users can be suspended")
        void suspendRequiresActive() {
            User user = newUserPending();
            assertThatThrownBy(() -> user.suspend(FIXED_CLOCK))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("deactivate: only ACTIVE users can be deactivated")
        void deactivateRequiresActive() {
            User user = newUserPending();
            assertThatThrownBy(() -> user.deactivate(FIXED_CLOCK))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("deactivate: DEACTIVATED is terminal")
        void deactivatedIsTerminal() {
            User user = newUserPending();
            user.activate(FIXED_CLOCK);
            user.deactivate(FIXED_CLOCK);

            assertThatThrownBy(() -> user.deactivate(FIXED_CLOCK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already DEACTIVATED");
        }
    }

    @Test
    @DisplayName("createdAt never changes; updatedAt advances on each transition")
    void updatedAtAdvances() {
        Clock movingClock = Clock.system(ZoneId.of("UTC"));
        User user = User.register(
                Email.of("bob@example.com"),
                PasswordHash.of("$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMN"),
                Roles.of(Role.CUSTOMER),
                movingClock);

        Instant createdAt = user.createdAt();
        try { Thread.sleep(5); } catch (InterruptedException ignored) {}

        user.activate(movingClock);
        assertThat(user.createdAt()).isEqualTo(createdAt);
        assertThat(user.updatedAt()).isAfter(createdAt);
    }
}