package com.engine.identity.domain.exception;

import com.engine.identity.domain.model.Email;

/**
 * Thrown by {@link com.engine.identity.domain.port.in.RegisterUserUseCase} when a user with the
 * given email already exists. Email is the natural key of the {@link com.engine.identity.domain.model.User}
 * aggregate.
 */
public final class EmailAlreadyRegisteredException extends RuntimeException {

    private final Email email;

    public EmailAlreadyRegisteredException(Email email) {
        super("A user is already registered with email: " + email.value());
        this.email = email;
    }

    public Email email() {
        return email;
    }
}