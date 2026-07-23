package com.engine.identity.application;

import com.engine.identity.domain.exception.InvalidCredentialsException;
import com.engine.identity.domain.exception.UserNotActiveException;
import com.engine.identity.domain.model.Email;
import com.engine.identity.domain.model.User;
import com.engine.identity.domain.port.in.AuthenticateUserUseCase.AuthenticateUserCommand;
import com.engine.identity.domain.model.JwtTokens;
import com.engine.identity.domain.model.PasswordHash;
import com.engine.identity.domain.model.Role;
import com.engine.identity.domain.model.Roles;
import com.engine.identity.domain.model.UserStatus;
import com.engine.shared.domain.ids.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthenticateUserService")
class AuthenticateUserServiceTest {

    private FakeUserRepository userRepository;
    private FakePasswordHasher passwordHasher;
    private FakeJwtIssuer jwtIssuer;
    private AuthenticateUserService service;

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        passwordHasher = new FakePasswordHasher();
        jwtIssuer = new FakeJwtIssuer();
        service = new AuthenticateUserService(userRepository, passwordHasher, jwtIssuer);
    }

    private User seedActiveUser(String email, String rawPassword) {
        User user = User.register(
                Email.of(email),
                passwordHasher.hash(rawPassword),
                Roles.of(Role.CUSTOMER),
                FIXED_CLOCK);
        user.activate(FIXED_CLOCK);
        userRepository.save(user);
        user.clearEvents();
        return user;
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("returns JWT tokens for correct credentials on an ACTIVE user")
        void returnsTokensForActiveUser() {
            seedActiveUser("alice@example.com", "p@ssw0rd");

            JwtTokens tokens = service.authenticate(
                    new AuthenticateUserCommand(Email.of("alice@example.com"), "p@ssw0rd"));

            assertThat(tokens.accessToken()).isEqualTo(FakeJwtIssuer.ACCESS_TOKEN);
            assertThat(tokens.refreshToken()).isEqualTo(FakeJwtIssuer.REFRESH_TOKEN);
        }
    }

    @Nested
    @DisplayName("credential failures")
    class CredentialFailures {

        @Test
        @DisplayName("unknown email throws InvalidCredentialsException")
        void unknownEmail() {
            assertThatThrownBy(() -> service.authenticate(
                    new AuthenticateUserCommand(Email.of("nobody@example.com"), "p@ssw0rd")))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("wrong password throws InvalidCredentialsException")
        void wrongPassword() {
            seedActiveUser("alice@example.com", "p@ssw0rd");

            assertThatThrownBy(() -> service.authenticate(
                    new AuthenticateUserCommand(Email.of("alice@example.com"), "wr0ng")))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("message does not reveal whether email or password was wrong (no enumeration)")
        void messageIsIdenticalForEmailAndPassword() {
            seedActiveUser("alice@example.com", "p@ssw0rd");

            String unknownEmailMessage = catchMessage(() -> service.authenticate(
                    new AuthenticateUserCommand(Email.of("nobody@example.com"), "p@ssw0rd")));
            String wrongPasswordMessage = catchMessage(() -> service.authenticate(
                    new AuthenticateUserCommand(Email.of("alice@example.com"), "wr0ng")));

            assertThat(unknownEmailMessage).isEqualTo(wrongPasswordMessage);
        }
    }

    @Nested
    @DisplayName("lifecycle failures")
    class LifecycleFailures {

        @Test
        @DisplayName("PENDING user throws UserNotActiveException")
        void pendingUserRejected() {
            User pending = User.register(
                    Email.of("bob@example.com"),
                    passwordHasher.hash("p@ssw0rd"),
                    Roles.of(Role.CUSTOMER),
                    FIXED_CLOCK);
            userRepository.save(pending);
            pending.clearEvents();

            assertThatThrownBy(() -> service.authenticate(
                    new AuthenticateUserCommand(Email.of("bob@example.com"), "p@ssw0rd")))
                    .isInstanceOf(UserNotActiveException.class)
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("SUSPENDED user throws UserNotActiveException")
        void suspendedUserRejected() {
            User user = seedActiveUser("carol@example.com", "p@ssw0rd");
            user.suspend(FIXED_CLOCK);
            userRepository.save(user);

            assertThatThrownBy(() -> service.authenticate(
                    new AuthenticateUserCommand(Email.of("carol@example.com"), "p@ssw0rd")))
                    .isInstanceOf(UserNotActiveException.class)
                    .hasMessageContaining("SUSPENDED");
        }
    }

    @Nested
    @DisplayName("null arguments")
    class NullChecks {

        @Test
        @DisplayName("constructor rejects null ports")
        void constructorRejectsNulls() {
            assertThatThrownBy(() -> new AuthenticateUserService(null, passwordHasher, jwtIssuer))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new AuthenticateUserService(userRepository, null, jwtIssuer))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new AuthenticateUserService(userRepository, passwordHasher, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    private String catchMessage(Runnable runnable) {
        try {
            runnable.run();
            throw new AssertionError("Expected InvalidCredentialsException");
        } catch (InvalidCredentialsException e) {
            return e.getMessage();
        }
    }
}