package com.engine.payment.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("paymentIdempotencyJpaRepository")
public interface IdempotencyJpaRepository extends JpaRepository<IdempotencyEntity, String> {
    Optional<IdempotencyEntity> findByIdempotencyKey(String idempotencyKey);
}
