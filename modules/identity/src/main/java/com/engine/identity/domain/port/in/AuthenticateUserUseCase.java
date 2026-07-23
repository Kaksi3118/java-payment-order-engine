package com.engine.identity.domain.port.in;

import com.engine.identity.domain.model.Email;
import com.engine.identity.domain.model.JwtTokens;

import java.util.Objects;

/**
 * Driving port (use-case contract): authenticate an existing user and issue JWT tokens.
 */
public interface AuthenticateUserUseCase {

    JwtTokens authenticate(AuthenticateUserCommand command);

    /**
     * Input to authentication: an email and the raw plaintext password the user is attempting
     * to log in with. The raw password is never logged and never persisted; it is fed straight
     * to the {@link com.engine.identity.domain.port.out.PasswordHasher#matches} driven port.
     */
    record AuthenticateUserCommand(Email email, String rawPassword) {

        public AuthenticateUserCommand {
            Objects.requireNonNull(email, "email must not be null");
            Objects.requireNonNull(rawPassword, "rawPassword must not be null");
            if (rawPassword.isBlank()) {
                throw new IllegalArgumentException("rawPassword must not be blank");
            }
        }
    }
}