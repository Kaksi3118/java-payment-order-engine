package com.engine.payment.application;

import com.engine.payment.domain.exception.IdempotencyConflictException;
import com.engine.payment.domain.port.out.IdempotencyPort;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class FakeIdempotencyPort implements IdempotencyPort {

    record Entry(String hash, String result) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public Optional<String> findResult(String key, String requestHash) {
        Entry entry = store.get(key);
        if (entry != null) {
            if (!entry.hash().equals(requestHash)) {
                throw new IdempotencyConflictException(key);
            }
            return Optional.of(entry.result());
        }
        return Optional.empty();
    }

    @Override
    public void saveResult(String key, String requestHash, String resultJson) {
        store.put(key, new Entry(requestHash, resultJson));
    }
}
