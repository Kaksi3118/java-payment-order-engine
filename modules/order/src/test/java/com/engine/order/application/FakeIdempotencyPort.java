package com.engine.order.application;

import com.engine.order.domain.exception.IdempotencyConflictException;
import com.engine.order.domain.port.out.IdempotencyPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Fake {@link IdempotencyPort} for unit tests.
 *
 * <p>Stores key &rarr; (hash, result) pairs in a map. {@code findResult} returns the cached
 * result if the key AND hash match; throws {@link IdempotencyConflictException} if the key
 * exists but the hash differs (same key used for a different request body).
 */
final class FakeIdempotencyPort implements IdempotencyPort {

    private record Entry(String hash, String result) {}

    private final Map<String, Entry> store = new HashMap<>();

    @Override
    public Optional<String> findResult(String key, String requestHash) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(requestHash);
        Entry entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (!entry.hash().equals(requestHash)) {
            throw new IdempotencyConflictException(key);
        }
        return Optional.of(entry.result());
    }

    @Override
    public void saveResult(String key, String requestHash, String resultJson) {
        store.put(key, new Entry(requestHash, resultJson));
    }

    int size() {
        return store.size();
    }
}