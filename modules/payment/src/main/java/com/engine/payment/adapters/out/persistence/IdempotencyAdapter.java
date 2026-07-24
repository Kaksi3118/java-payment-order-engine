package com.engine.payment.adapters.out.persistence;

import com.engine.payment.domain.exception.IdempotencyConflictException;
import com.engine.payment.domain.port.out.IdempotencyPort;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Component("paymentIdempotencyAdapter")
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
