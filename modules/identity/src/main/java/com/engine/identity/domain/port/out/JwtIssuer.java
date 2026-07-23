package com.engine.identity.domain.port.out;

import com.engine.identity.domain.model.JwtTokens;
import com.engine.identity.domain.model.User;

/**
 * Driven port: encodes a {@link User}'s identity into JWT access and refresh tokens.
 *
 * <p>The domain only constrains the contract: two opaque strings with finite expiry. The adapter
 * (typically Spring Security's {@code JwtEncoder} backed by a symmetric key or JWK set) chooses
 * the algorithm, claim shape, and serialization format. Cross-context consumers verify the
 * access token at the resource server; the Identity context owns issuance and rotation.
 */
public interface JwtIssuer {

    JwtTokens issue(User user);
}