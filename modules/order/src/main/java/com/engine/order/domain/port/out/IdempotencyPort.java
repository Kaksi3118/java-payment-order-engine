package com.engine.order.domain.port.out;

import java.util.Optional;

/**
 * Driven port: idempotency key store.
 *
 * <p>Implements the Idempotency Pattern: every mutating endpoint accepts an
 * {@code Idempotency-Key} header. The application layer calls {@link #findResult} before
 * executing the command; if a cached result exists for the same key AND the same request
 * hash, it is returned directly without re-executing the side effect. After successful
 * execution, {@link #saveResult} persists the key + result for future replays.
 *
 * <p>Implementations live in {@code adapters.out} and may be backed by Redis or a
 * dedicated database table. The store MUST be atomic: a concurrent {@code findResult}
 * + {@code saveResult} race must not allow two requests with the same key to both
 * execute the command.
 */
public interface IdempotencyPort {

    /**
     * Returns the cached result for the given key, if any.
     *
     * @param key          the idempotency key from the request header
     * @param requestHash  a hash of the request body to detect key reuse for different payloads
     * @return the cached result, or empty if no prior request used this key
     * @throws com.engine.order.domain.exception.IdempotencyConflictException if the key was
     *         already used for a different request body (different hash)
     */
    Optional<String> findResult(String key, String requestHash);

    /**
     * Persists the result for the given key so future replays return the cached value.
     *
     * @param key          the idempotency key
     * @param requestHash  a hash of the request body
     * @param resultJson   the serialized result to return on replay
     */
    void saveResult(String key, String requestHash, String resultJson);
}