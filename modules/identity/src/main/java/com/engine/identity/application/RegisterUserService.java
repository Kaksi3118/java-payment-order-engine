package com.engine.identity.application;

import com.engine.identity.domain.exception.EmailAlreadyRegisteredException;
import com.engine.identity.domain.model.Email;
import com.engine.identity.domain.model.PasswordHash;
import com.engine.identity.domain.model.User;
import com.engine.identity.domain.port.in.RegisterUserUseCase;
import com.engine.shared.domain.port.out.EventOutbox;
import com.engine.identity.domain.port.out.PasswordHasher;
import com.engine.identity.domain.port.out.UserRepository;
import com.engine.shared.domain.event.DomainEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;

/**
 * Use case: register a new user.
 *
 * <p>Orchestrates the driven ports in a single transaction:
 * <ol>
 *     <li>Reject if the email is already registered (natural-key uniqueness check).</li>
 *     <li>Hash the raw password via {@link PasswordHasher} &mdash; the plaintext never reaches
 *         the {@link User} aggregate.</li>
 *     <li>Create the aggregate via {@link User#register} (PENDING status, raises
 *         {@code UserRegistered}).</li>
 *     <li>Persist the aggregate.</li>
 *     <li>Drain the aggregate's domain events to the {@link EventOutbox} so they are written
 *         in the <em>same</em> transaction &mdash; the Transactional Outbox Pattern.</li>
 * </ol>
 *
 * <p>If any step fails the entire transaction rolls back, including the outbox rows, so no
 * event is ever published for a user that was not persisted.
 */
@Service
public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final EventOutbox eventOutbox;
    private final Clock clock;

    public RegisterUserService(UserRepository userRepository,
                               PasswordHasher passwordHasher,
                               EventOutbox eventOutbox,
                               Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository must not be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "PasswordHasher must not be null");
        this.eventOutbox = Objects.requireNonNull(eventOutbox, "EventOutbox must not be null");
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
    }

    @Override
    @Transactional
    public RegisterUserResult register(RegisterUserCommand command) {
        Email email = command.email();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }

        PasswordHash hash = passwordHasher.hash(command.rawPassword());
        User user = User.register(email, hash, command.roles(), clock);
        userRepository.save(user);

        for (DomainEvent event : user.domainEvents()) {
            eventOutbox.append(event);
        }
        user.clearEvents();

        return new RegisterUserResult(user.idValue());
    }
}