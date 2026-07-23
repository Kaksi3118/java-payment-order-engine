package com.engine.identity.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link UserEntity}.
 *
 * <p>Query methods are derived from method names &mdash; no JPQL needed for this simple surface.
 * The {@link UserRepositoryAdapter} wraps this repository and handles domain&lt;-&gt;entity mapping.
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}