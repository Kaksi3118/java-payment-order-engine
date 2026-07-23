package com.engine.payment.domain.exception;

public class IdempotencyConflictException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public IdempotencyConflictException(String key) {
        super("Idempotency key conflict: the key '" + key + "' was already used for a different request payload.");
    }
}
