package com.engine.identity.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link OutboxEntity}.
 *
 * <p>Exposes the query the outbox poller (Stage 6) will use to lease PENDING rows ordered by
 * creation time. The poller will claim rows using a Redis distributed lock to ensure a single
 * dispatcher across application instances.
 */
public interface OutboxJpaRepository extends JpaRepository<OutboxEntity, UUID> {

    List<OutboxEntity> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}