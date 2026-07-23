package com.engine.identity.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Request body for {@code POST /api/auth/register}.
 *
 * <p>Validation annotations are evaluated by Spring's {@code @Valid} on the controller method.
 * The {@code password} field has a minimum-length constraint (8 characters) &mdash; the BCrypt
 * adapter will hash whatever survives validation; the domain never sees the raw value.
 */
public record RegisterRequest(
        @NotBlank(message = "email must not be blank")
        @Pattern(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "email must be a valid email address")
        String email,

        @NotBlank(message = "password must not be blank")
        @Pattern(regexp = ".{8,}", message = "password must be at least 8 characters")
        String password,

        @NotEmpty(message = "roles must not be empty")
        List<@NotBlank String> roles
) {
}