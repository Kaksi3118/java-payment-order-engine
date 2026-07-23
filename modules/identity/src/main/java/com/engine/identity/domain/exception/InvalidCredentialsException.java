package com.engine.identity.domain.exception;

/**
 * Thrown by {@link com.engine.identity.domain.port.in.AuthenticateUserUseCase} when the supplied
 * credentials do not match any known user. Treating &quot;wrong email&quot; and &quot;wrong
 * password&quot; identically is an intentional security choice: it forecloses user enumeration.
 */
public final class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}