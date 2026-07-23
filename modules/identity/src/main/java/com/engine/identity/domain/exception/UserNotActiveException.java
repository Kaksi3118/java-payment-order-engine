package com.engine.identity.domain.exception;

import com.engine.identity.domain.model.UserStatus;

/**
 * Thrown when authenticating a user whose status is not {@link UserStatus#ACTIVE}
 * (e.g. PENDING unverified, SUSPENDED, or DEACTIVATED). Separate from
 * {@link InvalidCredentialsException} because the credentials were correct &mdash; it is the
 * lifecycle that rejected them &mdash; but the message stays opaque to avoid user enumeration.
 */
public final class UserNotActiveException extends RuntimeException {

    private final UserStatus status;

    public UserNotActiveException(UserStatus status) {
        super("User account is not active; current status: " + status);
        this.status = status;
    }

    public UserStatus status() {
        return status;
    }
}