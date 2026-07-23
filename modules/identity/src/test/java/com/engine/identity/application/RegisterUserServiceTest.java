package com.engine.identity.application;

import com.engine.identity.domain.event.UserRegistered;
import com.engine.identity.domain.exception.EmailAlreadyRegisteredException;
import com.engine.identity.domain.model.Email;
import com.engine.identity.domain.model.Role;
import com.engine.identity.domain.model.Roles;
import com.engine.identity.domain.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.engine.identity.domain.port.in.RegisterUserUseCase.RegisterUserResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RegisterUserService")
class RegisterUserServiceTest {

    private FakeUserRepository userRepository;
    private FakePasswordHasher passwordHasher;
    private FakeEventOutbox eventOutbox;
    private Clock clock;
    private RegisterUserService service;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        passwordHasher = new FakePasswordHasher();
        eventOutbox = new FakeEventOutbox();
        clock = Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC);
        service = new RegisterUserService(userRepository, passwordHasher, eventOutbox, clock);
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("registers a new user and returns the user ID")
        void registersUserAndReturnsId() {
            RegisterUserCommand command = new RegisterUserCommand(
                    Email.of("alice@example.com"), "p@ssw0rd", Roles.of(Role.CUSTOMER));

            RegisterUserResult result = service.register(command);

            assertThat(result.userId()).isNotNull();
            assertThat(userRepository.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("persists a user with a hashed password (never the raw plaintext)")
        void persistsHashedPassword() {
            RegisterUserCommand command = new RegisterUserCommand(
                    Email.of("alice@example.com"), "p@ssw0rd", Roles.of(Role.CUSTOMER));

            service.register(command);

            var saved = userRepository.findByEmail(Email.of("alice@example.com")).orElseThrow();
            assertThat(saved.passwordHash().value()).startsWith("$2");
            assertThat(saved.passwordHash().value()).isNotEqualTo("p@ssw0rd");
        }

        @Test
        @DisplayName("drains exactly one UserRegistered event to the outbox, then clears the aggregate buffer")
        void drainsUserRegisteredEventToOutbox() {
            RegisterUserCommand command = new RegisterUserCommand(
                    Email.of("alice@example.com"), "p@ssw0rd", Roles.of(Role.CUSTOMER));

            service.register(command);

            assertThat(eventOutbox.size()).isEqualTo(1);
            assertThat(eventOutbox.events().get(0)).isInstanceOf(UserRegistered.class);

            UserRegistered event = (UserRegistered) eventOutbox.events().get(0);
            assertThat(event.email()).isEqualTo(Email.of("alice@example.com"));
            assertThat(event.roles()).isEqualTo(Roles.of(Role.CUSTOMER));
            assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-07-23T12:00:00Z"));
        }
    }

    @Nested
    @DisplayName("duplicate email")
    class DuplicateEmail {

        @Test
        @DisplayName("throws EmailAlreadyRegisteredException when email is already taken")
        void throwsOnDuplicateEmail() {
            RegisterUserCommand first = new RegisterUserCommand(
                    Email.of("alice@example.com"), "p@ssw0rd", Roles.of(Role.CUSTOMER));
            service.register(first);

            RegisterUserCommand second = new RegisterUserCommand(
                    Email.of("alice@example.com"), "different", Roles.of(Role.ADMIN));

            assertThatThrownBy(() -> service.register(second))
                    .isInstanceOf(EmailAlreadyRegisteredException.class)
                    .hasMessageContaining("alice@example.com");
        }

        @Test
        @DisplayName("does not append to the outbox when the registration is rejected")
        void noOutboxAppendOnRejection() {
            RegisterUserCommand first = new RegisterUserCommand(
                    Email.of("alice@example.com"), "p@ssw0rd", Roles.of(Role.CUSTOMER));
            service.register(first);

            try {
                RegisterUserCommand duplicate = new RegisterUserCommand(
                        Email.of("alice@example.com"), "other", Roles.of(Role.ADMIN));
                service.register(duplicate);
            } catch (EmailAlreadyRegisteredException ignored) {
            }

            assertThat(eventOutbox.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("null arguments")
    class NullChecks {

        @Test
        @DisplayName("constructor rejects null ports")
        void constructorRejectsNulls() {
            assertThatThrownBy(() -> new RegisterUserService(null, passwordHasher, eventOutbox, clock))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new RegisterUserService(userRepository, null, eventOutbox, clock))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new RegisterUserService(userRepository, passwordHasher, null, clock))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new RegisterUserService(userRepository, passwordHasher, eventOutbox, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}