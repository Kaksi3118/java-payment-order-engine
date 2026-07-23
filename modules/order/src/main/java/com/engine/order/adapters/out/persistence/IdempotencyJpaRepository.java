package com.engine.order.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link IdempotencyEntity}.
 */
public interface IdempotencyJpaRepository extends JpaRepository<IdempotencyEntity, String> {

    Optional<IdempotencyEntity> findByIdempotencyKey(String idempotencyKey);
}