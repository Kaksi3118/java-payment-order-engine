package com.engine.order.adapters.out.persistence;

import com.engine.order.domain.exception.IdempotencyConflictException;
import com.engine.order.domain.port.out.IdempotencyPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Adapter: implements the {@link IdempotencyPort} driven port via a JPA table.
 *
 * <p>The {@code idempotency_keys} table has a UNIQUE constraint on the key column, so a
 * concurrent insert race is resolved by the database itself. {@code findResult} returns
 * the cached result if the key AND hash match; throws {@link IdempotencyConflictException}
 * if the key exists but the hash differs (same key used for a different request body).
 */
@Component
public class IdempotencyAdapter implements IdempotencyPort {

    private final IdempotencyJpaRepository jpaRepository;
    private final Clock clock;

    public IdempotencyAdapter(IdempotencyJpaRepository jpaRepository, Clock clock) {
        this.jpaRepository = Objects.requireNonNull(jpaRepository, "IdempotencyJpaRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
    }

    @Override
    public Optional<String> findResult(String key, String requestHash) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(requestHash, "requestHash must not be null");

        return jpaRepository.findByIdempotencyKey(key).map(entity -> {
            if (!entity.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(key);
            }
            return entity.getResultJson();
        });
    }

    @Override
    public void saveResult(String key, String requestHash, String resultJson) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(requestHash, "requestHash must not be null");
        Objects.requireNonNull(resultJson, "resultJson must not be null");

        IdempotencyEntity entity = new IdempotencyEntity(key, requestHash, resultJson, Instant.now(clock));
        jpaRepository.save(entity);
    }
}