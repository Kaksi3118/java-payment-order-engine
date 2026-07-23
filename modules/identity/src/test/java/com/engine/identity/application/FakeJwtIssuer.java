package com.engine.identity.application;

import com.engine.identity.domain.model.JwtTokens;
import com.engine.identity.domain.model.User;
import com.engine.identity.domain.port.out.JwtIssuer;

import java.time.Instant;

/**
 * Fake {@link JwtIssuer} for unit tests.
 *
 * <p>Returns a canned {@link JwtTokens} value object. The test asserts on the returned
 * token strings to verify the use case delegates to the issuer, without needing a real
 * JWT library or signing key.
 */
final class FakeJwtIssuer implements JwtIssuer {

    static final String ACCESS_TOKEN = "fake-access-token";
    static final String REFRESH_TOKEN = "fake-refresh-token";

    @Override
    public JwtTokens issue(User user) {
        return new JwtTokens(
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                Instant.parse("2026-07-23T13:00:00Z"),
                Instant.parse("2026-07-24T12:00:00Z"));
    }
}