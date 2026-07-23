package com.engine.identity.adapters.out.security;

import com.engine.identity.domain.model.PasswordHash;
import com.engine.identity.domain.port.out.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Adapter: BCrypt implementation of the {@link PasswordHasher} driven port.
 *
 * <p>Wraps Spring Security's {@link BCryptPasswordEncoder}. Each call to {@link #hash} produces a
 * fresh random salt (BCrypt's default behavior), so two hashes of the same raw password are
 * <em>never</em> byte-equal &mdash; an essential property this adapter's unit test asserts.
 *
 * <p>Encoding strength is BCrypt's default (10 rounds). Login throughput is CPU-bounded by this
 * parameter, so high-traffic deployments should benchmark raising it to 12+.
 */
@Component
public class BcryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public PasswordHash hash(String rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword must not be null");
        return PasswordHash.of(encoder.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
        Objects.requireNonNull(rawPassword, "rawPassword must not be null");
        Objects.requireNonNull(hash, "hash must not be null");
        return encoder.matches(rawPassword, hash.value());
    }
}