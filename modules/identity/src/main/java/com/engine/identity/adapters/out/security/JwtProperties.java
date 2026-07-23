package com.engine.identity.adapters.out.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

/**
 * Bound to properties under {@code identity.jwt.*} in {@code application.yml}. Constructor-bound
 * (immutable) and self-validating: the refresh-token TTL must strictly exceed the access-token
 * TTL, otherwise an {@link IllegalArgumentException} fails application startup fast.
 *
 * <p>Production caveat: this showcase generates a new RSA keypair on every application startup
 * (see {@link JwtConfig}), invalidating previously issued tokens on every restart. For production
 * the keypair should be loaded from a JWK set URL or PKCS#8 keystore file so issued tokens
 * survive restarts.
 */
@ConfigurationProperties(prefix = "identity.jwt")
public record JwtProperties(Duration accessTokenTtl, Duration refreshTokenTtl) {

    public JwtProperties {
        Objects.requireNonNull(accessTokenTtl, "identity.jwt.access-token-ttl must not be null");
        Objects.requireNonNull(refreshTokenTtl, "identity.jwt.refresh-token-ttl must not be null");
        if (accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("identity.jwt.access-token-ttl must be positive");
        }
        if (refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
            throw new IllegalArgumentException("identity.jwt.refresh-token-ttl must be positive");
        }
        if (refreshTokenTtl.compareTo(accessTokenTtl) <= 0) {
            throw new IllegalArgumentException(
                    "identity.jwt.refresh-token-ttl must be greater than access-token-ttl");
        }
    }
}