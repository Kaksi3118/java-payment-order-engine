package com.engine.order.adapters.in.rest.dto;

import java.time.Instant;

/**
 * Standard error response body for the Order API.
 * Same shape as the Identity context's ErrorResponse for API consistency.
 */
public record ErrorResponse(int status, String error, String message, Instant timestamp) {
}