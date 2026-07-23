package com.engine.identity.adapters.out.persistence;

import com.engine.identity.domain.model.Email;
import com.engine.identity.domain.model.PasswordHash;
import com.engine.identity.domain.model.Role;
import com.engine.identity.domain.model.Roles;
import com.engine.identity.domain.model.User;
import com.engine.identity.domain.model.UserStatus;
import com.engine.identity.domain.port.out.UserRepository;
import com.engine.shared.domain.ids.UserId;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter: implements the {@link UserRepository} driven port via Spring Data JPA.
 *
 * <p>Handles bidirectional mapping between the domain {@link User} aggregate and the
 * {@link UserEntity} JPA entity. For {@link #save}, existing entities are loaded first so the
 * JPA {@code @Version} is preserved &mdash; a naive {@code merge} from a fresh entity would lose
 * the version and defeat optimistic locking.
 */
@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        Optional<UserEntity> existing = jpaRepository.findById(user.idValue());
        UserEntity entity = existing
                .map(e -> updateExisting(e, user))
                .orElseGet(() -> toNewEntity(user));
        UserEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.value());
    }

    private UserEntity toNewEntity(User user) {
        return new UserEntity(
                user.idValue(),
                user.email().value(),
                user.passwordHash().value(),
                user.roles().values(),
                user.status(),
                user.createdAt(),
                user.updatedAt());
    }

    private UserEntity updateExisting(UserEntity entity, User user) {
        entity.setPasswordHash(user.passwordHash().value());
        entity.setRoles(user.roles().values());
        entity.setStatus(user.status());
        entity.setUpdatedAt(user.updatedAt());
        return entity;
    }

    private User toDomain(UserEntity entity) {
        return User.reconstitute(
                UserId.of(entity.getId()),
                Email.of(entity.getEmail()),
                PasswordHash.of(entity.getPasswordHash()),
                Roles.from(entity.getRoles()),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}