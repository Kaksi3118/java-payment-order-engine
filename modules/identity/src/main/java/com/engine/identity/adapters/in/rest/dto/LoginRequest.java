package com.engine.identity.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/auth/login}.
 */
public record LoginRequest(
        @NotBlank(message = "email must not be blank")
        String email,

        @NotBlank(message = "password must not be blank")
        String password
) {
}