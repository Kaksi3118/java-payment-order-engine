package com.engine.identity.adapters.out.security;

import com.engine.identity.domain.model.Email;
import com.engine.identity.domain.model.JwtTokens;
import com.engine.identity.domain.model.PasswordHash;
import com.engine.identity.domain.model.Role;
import com.engine.identity.domain.model.Roles;
import com.engine.identity.domain.model.User;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.within;

import java.time.temporal.ChronoUnit;

@DisplayName("JwtIssuerAdapter")
class JwtIssuerAdapterTest {

    private static final Duration ACCESS_TTL = Duration.ofMinutes(30);
    private static final Duration REFRESH_TTL = Duration.ofHours(24);
    private static final Duration TOLERANCE = Duration.ofSeconds(2);

    private JwtDecoder jwtDecoder;
    private JwtIssuerAdapter issuer;
    private User activeUser;
    private Instant setupInstant;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();

        JwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        jwtDecoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        JwtProperties jwtProperties = new JwtProperties(ACCESS_TTL, REFRESH_TTL);
        Clock clock = Clock.systemUTC();
        issuer = new JwtIssuerAdapter(jwtEncoder, jwtProperties, clock);

        setupInstant = Instant.now();
        activeUser = User.register(
                Email.of("alice@example.com"),
                PasswordHash.of("$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMN"),
                Roles.of(Role.CUSTOMER, Role.ADMIN),
                clock);
        activeUser.activate(clock);
        activeUser.clearEvents();
    }

    @Test
    @DisplayName("access token is RS256-decodable and carries sub, email, roles, typ=access")
    void accessTokenIsDecodableWithExpectedClaims() {
        JwtTokens tokens = issuer.issue(activeUser);

        Jwt decoded = jwtDecoder.decode(tokens.accessToken());

        assertThat(decoded.getSubject()).isEqualTo(activeUser.idValue().toString());
        assertThat(decoded.getClaimAsString(JwtIssuerAdapter.EMAIL_CLAIM)).isEqualTo("alice@example.com");
        assertThat(decoded.getClaimAsStringList(JwtIssuerAdapter.ROLES_CLAIM))
                .containsExactly("CUSTOMER", "ADMIN");
        assertThat(decoded.getClaimAsString(JwtIssuerAdapter.TYPE_CLAIM)).isEqualTo(JwtIssuerAdapter.TYPE_ACCESS);
        // Jwt.getIssuer() URL-coerces the iss claim; assert on the raw String form instead.
        assertThat(decoded.getClaimAsString(JwtClaimNames.ISS)).isEqualTo(JwtIssuerAdapter.ISSUER);

        assertThat(decoded.getIssuedAt()).isCloseTo(setupInstant, within(2, ChronoUnit.SECONDS));
        assertThat(decoded.getExpiresAt()).isCloseTo(setupInstant.plus(ACCESS_TTL), within(2, ChronoUnit.SECONDS));

        assertThat(tokens.accessTokenExpiresAt()).isEqualTo(decoded.getExpiresAt());
    }

    @Test
    @DisplayName("refresh token minimizes claims: only sub and typ=refresh, no email/roles")
    void refreshTokenMinimizesClaims() {
        JwtTokens tokens = issuer.issue(activeUser);

        Jwt decoded = jwtDecoder.decode(tokens.refreshToken());

        assertThat(decoded.getSubject()).isEqualTo(activeUser.idValue().toString());
        assertThat(decoded.getClaimAsString(JwtIssuerAdapter.TYPE_CLAIM)).isEqualTo(JwtIssuerAdapter.TYPE_REFRESH);
        assertThat(decoded.getExpiresAt()).isCloseTo(setupInstant.plus(REFRESH_TTL), within(2, ChronoUnit.SECONDS));
        assertThat(tokens.refreshTokenExpiresAt()).isEqualTo(decoded.getExpiresAt());

        assertThat((Object) decoded.getClaim(JwtIssuerAdapter.EMAIL_CLAIM)).isNull();
        assertThat((Object) decoded.getClaim(JwtIssuerAdapter.ROLES_CLAIM)).isNull();
    }

    @Test
    @DisplayName("access and refresh tokens are distinct strings (not byte-equal)")
    void accessAndRefreshTokensAreDistinct() {
        JwtTokens tokens = issuer.issue(activeUser);
        assertThat(tokens.accessToken()).isNotEqualTo(tokens.refreshToken());
    }

    @Test
    @DisplayName("access token expiry strictly precedes refresh token expiry")
    void accessExpiresBeforeRefresh() {
        JwtTokens tokens = issuer.issue(activeUser);
        assertThat(tokens.accessTokenExpiresAt()).isBefore(tokens.refreshTokenExpiresAt());
    }

    @Test
    @DisplayName("constructor rejects null ports")
    void constructorRejectsNullPorts() throws Exception {
        JwtProperties props = new JwtProperties(ACCESS_TTL, REFRESH_TTL);
        Clock clock = Clock.systemUTC();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey())));

        assertThatNullPointerException()
                .isThrownBy(() -> new JwtIssuerAdapter(null, props, clock));
        assertThatNullPointerException()
                .isThrownBy(() -> new JwtIssuerAdapter(encoder, null, clock));
        assertThatNullPointerException()
                .isThrownBy(() -> new JwtIssuerAdapter(encoder, props, null));
    }

    private static RSAKey rsaKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }
}