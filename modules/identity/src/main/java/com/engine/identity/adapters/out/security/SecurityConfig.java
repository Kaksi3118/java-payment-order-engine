package com.engine.identity.adapters.out.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the Identity context's HTTP endpoints.
 *
 * <p>Stateless (JWT-based) session management; CSRF disabled because the API is cookie-less and
 * runs behind a same-origin policy in production; {@code /api/auth/**} is {@code permitAll}
 * because registration and login are obviously pre-auth; every other request requires
 * authentication, validated by Spring Security's OAuth2 JWT resource server filter chain using
 * the {@link JwtConfig#jwtDecoder} bean.
 *
 * <p>{@link EnableConfigurationProperties} binds {@link JwtProperties} from
 * {@code identity.jwt.*} in {@code application.yml} and fails application startup fast if the
 * properties are malformed (see {@link JwtProperties}'s compact constructor).
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}