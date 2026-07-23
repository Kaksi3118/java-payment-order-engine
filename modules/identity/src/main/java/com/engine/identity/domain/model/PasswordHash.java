package com.engine.identity.domain.model;

import java.util.Objects;

/**
 * Opaque wrapper around a salted password hash produced by a {@link com.engine.identity.domain.port.out.PasswordHasher}.
 *
 * <p>The domain never accepts or holds a plaintext password. The use-case layer receives
 * a raw credential from the {@code RegisterUserCommand} and immediately delegates it to the
 * {@code PasswordHasher} driven port, never passing the raw string into the {@link User}
 * aggregate. This keeps the credential surface bounded to a single port.
 *
 * <p>The hash format is opaque to the domain &mdash; the BCrypt prefix check here is a
 * <em>defensive</em> invariant (cheap, never falsy) and does not couple the domain to BCrypt
 * algorithm internals. Switching to Argon2 in the adapter only requires changing this
 * prefix constant.
 */
public record PasswordHash(String value) {

    private static final String HASH_PREFIX = "$2";

    public PasswordHash {
        Objects.requireNonNull(value, "password hash must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("password hash must not be blank");
        }
        if (!value.startsWith(HASH_PREFIX)) {
            throw new IllegalArgumentException("Unsupported password hash format (expected BCrypt '$2...')");
        }
    }

    public static PasswordHash of(String value) {
        return new PasswordHash(value);
    }
}