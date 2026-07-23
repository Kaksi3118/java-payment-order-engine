package com.engine.identity.adapters.in.rest.dto;

import java.time.Instant;

/**
 * Standard error response body returned by the global exception handler.
 *
 * <p>Carries the HTTP status, a short machine-readable {@code error} string, a human-readable
 * {@code message}, and the timestamp. This shape is consistent across all error responses so
 * API consumers can parse errors uniformly.
 */
public record ErrorResponse(int status, String error, String message, Instant timestamp) {
}