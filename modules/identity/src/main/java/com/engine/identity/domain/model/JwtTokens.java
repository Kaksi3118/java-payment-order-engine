package com.engine.identity.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Result value object returned by the {@link com.engine.identity.domain.port.in.AuthenticateUserUseCase}
 * and produced by the {@link com.engine.identity.domain.port.out.JwtIssuer} driven port.
 *
 * <p>Holds an opaque JWT access token and refresh token plus their expiry instants. The domain
 * deliberately treats the token strings as opaque ({@link String}); JWT encoding &mdash; the
 * signature algorithm, claims, and serialization &mdash; is the responsibility of the
 * adapter that implements {@code JwtIssuer}. The domain only cares about the <em>contract</em>
 * that issued tokens have a finite lifetime and a paired refresh.
 */
public record JwtTokens(String accessToken, String refreshToken,
                        Instant accessTokenExpiresAt, Instant refreshTokenExpiresAt) {

    public JwtTokens {
        Objects.requireNonNull(accessToken, "access token must not be null");
        Objects.requireNonNull(refreshToken, "refresh token must not be null");
        Objects.requireNonNull(accessTokenExpiresAt, "access token expiry must not be null");
        Objects.requireNonNull(refreshTokenExpiresAt, "refresh token expiry must not be null");
        if (accessToken.isBlank()) {
            throw new IllegalArgumentException("access token must not be blank");
        }
        if (refreshToken.isBlank()) {
            throw new IllegalArgumentException("refresh token must not be blank");
        }
    }
}