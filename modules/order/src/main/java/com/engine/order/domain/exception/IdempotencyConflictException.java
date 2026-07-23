package com.engine.order.domain.exception;

/**
 * Thrown when an idempotency key has already been used for a different request body.
 * This indicates a client is reusing the same key for different operations &mdash; a
 * protocol violation that must be surfaced as HTTP 409 Conflict.
 */
public final class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String key) {
        super("Idempotency key '" + key + "' was already used for a different request");
    }
}