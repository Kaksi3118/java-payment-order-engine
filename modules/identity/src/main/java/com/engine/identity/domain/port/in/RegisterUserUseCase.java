package com.engine.identity.domain.port.in;

import com.engine.identity.domain.model.Email;
import com.engine.identity.domain.model.Roles;

import java.util.Objects;
import java.util.UUID;

/**
 * Driving port (use-case contract): register a new user.
 *
 * <p>The accompanying {@link RegisterUserCommand} record lives next to the interface deliberately
 * &mdash; the domain owns the <em>shape</em> of what it can be asked to do so that {@code domain}
 * never needs to import from the {@code application} package (an ArchUnit-guarded invariant in
 * {@code ArchitectureArchTest}).
 */
public interface RegisterUserUseCase {

    RegisterUserResult register(RegisterUserCommand command);

    /**
     * Input to the registration use case. Carries the raw plaintext password because the hash
     * has not been computed yet &mdash; the application layer will route it straight into the
     * {@link com.engine.identity.domain.port.out.PasswordHasher} driven port.
     */
    record RegisterUserCommand(Email email, String rawPassword, Roles roles) {

        public RegisterUserCommand {
            Objects.requireNonNull(email, "email must not be null");
            Objects.requireNonNull(rawPassword, "rawPassword must not be null");
            Objects.requireNonNull(roles, "roles must not be null");
            if (rawPassword.isBlank()) {
                throw new IllegalArgumentException("rawPassword must not be blank");
            }
        }
    }

    /**
     * Output of registration: the new user's identifier. Never returns the entity itself,
     * because the aggregate is a domain concern and REST/MVC layer should not hold a reference
     * to it (evolution risk).
     */
    record RegisterUserResult(UUID userId) {

        public RegisterUserResult {
            Objects.requireNonNull(userId, "userId must not be null");
        }
    }
}