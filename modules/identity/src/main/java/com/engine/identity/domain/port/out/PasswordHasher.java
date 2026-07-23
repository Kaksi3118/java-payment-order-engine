package com.engine.identity.domain.port.out;

import com.engine.identity.domain.model.PasswordHash;

/**
 * Driven port: produces and verifies password hashes.
 *
 * <p>Live implementations live in {@code adapters.out} (BCryptPasswordEncoder adapter). The
 * domain calls this port from use cases so plaintext credentials never reach the
 * {@link com.engine.identity.domain.model.User} aggregate.
 */
public interface PasswordHasher {

    PasswordHash hash(String rawPassword);

    boolean matches(String rawPassword, PasswordHash hash);
}