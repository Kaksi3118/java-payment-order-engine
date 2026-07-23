/**
 * Persistence adapters &mdash; JPA implementations of the Identity context's driven ports.
 *
 * <p>Contains two adapters:
 * <ul>
 *     <li>{@link com.engine.identity.adapters.out.persistence.UserRepositoryAdapter} &mdash;
 *         implements {@link com.engine.identity.domain.port.out.UserRepository} via Spring Data JPA.</li>
 *     <li>{@link com.engine.identity.adapters.out.persistence.OutboxAdapter} &mdash;
 *         implements {@link com.engine.shared.domain.port.out.EventOutbox} via a JPA outbox table.</li>
 * </ul>
 *
 * <p>Domain-to-entity mapping is explicit and bidirectional. The domain {@code User} aggregate
 * is reconstructed via {@link com.engine.identity.domain.model.User#reconstitute} (no events raised).
 * JPA's {@code @Version} on {@link com.engine.identity.adapters.out.persistence.UserEntity}
 * provides optimistic locking at the persistence layer.
 */
package com.engine.identity.adapters.out.persistence;