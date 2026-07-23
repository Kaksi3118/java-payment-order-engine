package com.engine.identity.adapters.in.rest.dto;

import java.util.UUID;

/**
 * Response body for {@code POST /api/auth/register} &mdash; the newly registered user's ID.
 */
public record RegisterResponse(UUID userId) {
}