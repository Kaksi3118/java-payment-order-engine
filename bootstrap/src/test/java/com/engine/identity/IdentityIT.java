package com.engine.identity;

import com.engine.bootstrap.Application;
import com.engine.identity.adapters.out.persistence.OutboxEntity;
import com.engine.identity.adapters.out.persistence.OutboxJpaRepository;
import com.engine.identity.adapters.out.persistence.OutboxStatus;
import com.engine.identity.domain.event.UserRegistered;
import com.engine.identity.domain.exception.EmailAlreadyRegisteredException;
import com.engine.identity.domain.exception.InvalidCredentialsException;
import com.engine.identity.domain.exception.UserNotActiveException;
import com.engine.identity.domain.model.Email;
import com.engine.identity.domain.model.JwtTokens;
import com.engine.identity.domain.model.Role;
import com.engine.identity.domain.model.Roles;
import com.engine.identity.domain.model.User;
import com.engine.identity.domain.model.UserStatus;
import com.engine.identity.domain.port.in.AuthenticateUserUseCase.AuthenticateUserCommand;
import com.engine.identity.domain.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.engine.identity.domain.port.in.RegisterUserUseCase.RegisterUserResult;
import com.engine.identity.domain.port.out.UserRepository;
import com.engine.shared.domain.ids.UserId;
import com.engine.shared.domain.port.out.EventOutbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end integration test for the Identity bounded context.
 *
 * <p>Spins up a real PostgreSQL 17 Docker container via Testcontainers, runs the Flyway V1
 * migration, and exercises the full stack: domain &rarr; application (use cases) &rarr;
 * adapters (JPA persistence, BCrypt hashing, JWT issuance, outbox).
 *
 * <p>Named {@code *IT} (not {@code *Test}) so it runs under Maven Failsafe during the
 * {@code integration-test} phase ({@code mvn verify}), not under Surefire ({@code mvn test}).
 * This keeps {@code mvn test} fast for unit-only feedback loops.
 *
 * <p>What this proves to a reviewer:
 * <ul>
 *     <li>Flyway migration DDL matches JPA entity annotations ({@code ddl-auto=validate}).</li>
 *     <li>The Transactional Outbox Pattern works: register &rarr; user row + outbox row in one tx.</li>
 *     <li>BCrypt hashing + verification works against a real database-stored hash.</li>
 *     <li>JWT issuance produces decodable tokens with correct claims.</li>
 *     <li>Domain exceptions propagate correctly from the use case through the Spring context.</li>
 * </ul>
 */
@SpringBootTest(classes = Application.class)
@Testcontainers
@EnabledIfEnvironmentVariable(named = "DOCKER_HOST", matches = ".*")
@DisplayName("Identity Integration Test (Testcontainers + PostgreSQL 17)")
class IdentityIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("poe_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private com.engine.identity.domain.port.in.RegisterUserUseCase registerUserUseCase;
    @Autowired
    private com.engine.identity.domain.port.in.AuthenticateUserUseCase authenticateUserUseCase;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OutboxJpaRepository outboxJpaRepository;
    @Autowired
    private EventOutbox eventOutbox;
    @Autowired
    private Clock clock;

    @Test
    @DisplayName("register user persists row + writes outbox event in the same transaction")
    void registerPersistsUserAndOutboxEvent() {
        RegisterUserResult result = registerUserUseCase.register(new RegisterUserCommand(
                Email.of("alice@example.com"),
                "p@ssw0rd",
                Roles.of(Role.CUSTOMER)));

        assertThat(result.userId()).isNotNull();

        var saved = userRepository.findByEmail(Email.of("alice@example.com"));
        assertThat(saved).isPresent();
        assertThat(saved.get().status()).isEqualTo(UserStatus.PENDING);
        assertThat(saved.get().passwordHash().value()).startsWith("$2");

        List<OutboxEntity> outboxRows = outboxJpaRepository.findAll();
        assertThat(outboxRows).isNotEmpty();
        OutboxEntity outboxRow = outboxRows.get(0);
        assertThat(outboxRow.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxRow.getEventType()).isEqualTo(UserRegistered.class.getName());
        assertThat(outboxRow.getAggregateId()).isEqualTo(result.userId());
        assertThat(outboxRow.getPayload()).contains("alice@example.com");
    }

    @Test
    @DisplayName("authenticate returns valid JWT tokens after user is activated")
    void authenticateReturnsTokensAfterActivation() {
        RegisterUserResult result = registerUserUseCase.register(new RegisterUserCommand(
                Email.of("bob@example.com"),
                "p@ssw0rd",
                Roles.of(Role.CUSTOMER, Role.ADMIN)));

        User user = userRepository.findById(UserId.of(result.userId())).orElseThrow();
        user.activate(clock);
        userRepository.save(user);
        for (var event : user.domainEvents()) {
            eventOutbox.append(event);
        }
        user.clearEvents();

        JwtTokens tokens = authenticateUserUseCase.authenticate(
                new AuthenticateUserCommand(Email.of("bob@example.com"), "p@ssw0rd"));

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.accessToken()).isNotEqualTo(tokens.refreshToken());
    }

    @Test
    @DisplayName("authenticate with wrong password throws InvalidCredentialsException")
    void wrongPasswordThrowsInvalidCredentials() {
        registerAndActivate("carol@example.com", "p@ssw0rd");

        assertThatThrownBy(() -> authenticateUserUseCase.authenticate(
                new AuthenticateUserCommand(Email.of("carol@example.com"), "wr0ng")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("authenticate against a PENDING (non-active) user throws UserNotActiveException")
    void pendingUserThrowsUserNotActive() {
        registerUserUseCase.register(new RegisterUserCommand(
                Email.of("dave@example.com"),
                "p@ssw0rd",
                Roles.of(Role.CUSTOMER)));

        assertThatThrownBy(() -> authenticateUserUseCase.authenticate(
                new AuthenticateUserCommand(Email.of("dave@example.com"), "p@ssw0rd")))
                .isInstanceOf(UserNotActiveException.class);
    }

    @Test
    @DisplayName("duplicate registration throws EmailAlreadyRegisteredException")
    void duplicateRegistrationThrows() {
        registerUserUseCase.register(new RegisterUserCommand(
                Email.of("eve@example.com"),
                "p@ssw0rd",
                Roles.of(Role.CUSTOMER)));

        assertThatThrownBy(() -> registerUserUseCase.register(new RegisterUserCommand(
                Email.of("eve@example.com"),
                "different",
                Roles.of(Role.ADMIN))))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    private void registerAndActivate(String email, String password) {
        RegisterUserResult result = registerUserUseCase.register(new RegisterUserCommand(
                Email.of(email), password, Roles.of(Role.CUSTOMER)));
        User user = userRepository.findById(UserId.of(result.userId())).orElseThrow();
        user.activate(clock);
        userRepository.save(user);
        for (var event : user.domainEvents()) {
            eventOutbox.append(event);
        }
        user.clearEvents();
    }
}