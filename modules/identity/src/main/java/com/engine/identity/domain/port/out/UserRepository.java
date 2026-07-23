package com.engine.identity.domain.port.out;

import com.engine.identity.domain.model.Email;
import com.engine.identity.domain.model.User;
import com.engine.shared.domain.ids.UserId;

import java.util.Optional;

/**
 * Driven port: persistence of the {@link User} aggregate.
 *
 * <p>Live adapters live in {@code adapters.out} (Spring Data JPA repository + entity mapping).
 * The contract is intentionally minimal &mdash; this is the only SQL surface the Identity
 * bounded context exposes, and everything outside it reads via events. Methods that mutate
 * ({@link #save}) MUST be invoked inside a transaction owned by the application layer so the
 * outbox row is written atomically with the aggregate state.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);
}