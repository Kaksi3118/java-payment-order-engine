package com.engine.identity.adapters.in.rest.dto;

/**
 * Response body for {@code POST /api/auth/login} &mdash; the issued JWT access and refresh tokens.
 */
public record LoginResponse(String accessToken, String refreshToken) {

    public static LoginResponse of(String accessToken, String refreshToken) {
        return new LoginResponse(accessToken, refreshToken);
    }
}