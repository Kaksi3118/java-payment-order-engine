package com.engine.identity.application;

import com.engine.identity.domain.model.PasswordHash;
import com.engine.identity.domain.port.out.PasswordHasher;

/**
 * Fake {@link PasswordHasher} for unit tests.
 *
 * <p>Produces hashes that satisfy the {@link PasswordHash} BCrypt-form invariant (prefix
 * {@code "$2"}) by embedding the raw password directly: {@code "$2a$fake$" + rawPassword}.
 * {@link #matches} checks that the stored hash ends with the supplied raw password. This is
 * cryptographically worthless but perfect for unit tests &mdash; no BCrypt library needed.
 */
final class FakePasswordHasher implements PasswordHasher {

    private static final String FAKE_PREFIX = "$2a$fake$";

    @Override
    public PasswordHash hash(String rawPassword) {
        return PasswordHash.of(FAKE_PREFIX + rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
        return hash.value().equals(FAKE_PREFIX + rawPassword);
    }
}