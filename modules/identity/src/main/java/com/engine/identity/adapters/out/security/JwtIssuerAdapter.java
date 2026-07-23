package com.engine.identity.adapters.out.security;

import com.engine.identity.domain.model.JwtTokens;
import com.engine.identity.domain.model.User;
import com.engine.identity.domain.port.out.JwtIssuer;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * Adapter: Spring Security OAuth2 JWT implementation of the {@link JwtIssuer} driven port.
 *
 * <p>Issues two RS256-signed tokens using the {@link JwtEncoder} of {@link JwtConfig}:
 * <ul>
 *     <li><strong>Access token</strong> &mdash; carries {@code sub=user.id}, {@code email},
 *         {@code roles}, and {@code typ=access}. Lives for {@link JwtProperties#accessTokenTtl()}.</li>
 *     <li><strong>Refresh token</strong> &mdash; carries only {@code sub=user.id} and
 *         {@code typ=refresh}. Lives for {@link JwtProperties#refreshTokenTtl()}.
 *         Deliberately omits {@code email}/{@code roles}: a leaked refresh token must not leak
 *         the user's identity beyond what the resource server can already derive from {@code sub}.</li>
 * </ul>
 *
 * <p>The {@code typ} claim is a JWT-type discriminator that prevents a refresh token from being
 * misread as an access token by a future resource server filter that respects it.
 */
@Component
public class JwtIssuerAdapter implements JwtIssuer {

    public static final String ISSUER = "https://payment-order-engine";
    public static final String TYPE_CLAIM = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    public static final String ROLES_CLAIM = "roles";
    public static final String EMAIL_CLAIM = "email";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public JwtIssuerAdapter(JwtEncoder jwtEncoder, JwtProperties jwtProperties, Clock clock) {
        this.jwtEncoder = Objects.requireNonNull(jwtEncoder, "JwtEncoder must not be null");
        this.jwtProperties = Objects.requireNonNull(jwtProperties, "JwtProperties must not be null");
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
    }

    @Override
    public JwtTokens issue(User user) {
        Objects.requireNonNull(user, "user must not be null");

        // Truncate to whole seconds because the JWT spec (RFC 7519) serializes numeric
        // claims (iat, exp) as seconds since the epoch, dropping nanoseconds. Truncating
        // here keeps the {@link JwtTokens#accessTokenExpiresAt()} promise consistent with
        // what a decoded JWT roundtrip yields, so {@code decoded.getExpiresAt().equals(tokens.accessTokenExpiresAt())}.
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        List<String> roleNames = user.roles().values().stream().map(Enum::name).toList();

        JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.idValue().toString())
                .claim(EMAIL_CLAIM, user.email().value())
                .claim(ROLES_CLAIM, roleNames)
                .claim(TYPE_CLAIM, TYPE_ACCESS)
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.accessTokenTtl()))
                .build();

        JwtClaimsSet refreshClaims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.idValue().toString())
                .claim(TYPE_CLAIM, TYPE_REFRESH)
                .issuedAt(now)
                .expiresAt(now.plus(jwtProperties.refreshTokenTtl()))
                .build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(accessClaims)).getTokenValue();
        String refreshToken = jwtEncoder.encode(JwtEncoderParameters.from(refreshClaims)).getTokenValue();

        return new JwtTokens(accessToken, refreshToken,
                accessClaims.getExpiresAt(), refreshClaims.getExpiresAt());
    }
}