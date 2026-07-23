package com.engine.identity.adapters.out.persistence;

import com.engine.identity.domain.model.Email;
import com.engine.identity.domain.model.PasswordHash;
import com.engine.identity.domain.model.Role;
import com.engine.identity.domain.model.Roles;
import com.engine.identity.domain.model.User;
import com.engine.identity.domain.model.UserStatus;
import com.engine.shared.domain.ids.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("UserRepositoryAdapter")
class UserRepositoryAdapterTest {

    private UserJpaRepository jpaRepository;
    private UserRepositoryAdapter adapter;

    private static final Instant CREATED_AT = Instant.parse("2026-07-23T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-23T12:30:00Z");

    @BeforeEach
    void setUp() {
        jpaRepository = mock(UserJpaRepository.class);
        adapter = new UserRepositoryAdapter(jpaRepository);
    }

    private User newUser(UserStatus status) {
        UserId id = UserId.random();
        return User.reconstitute(id,
                Email.of("alice@example.com"),
                PasswordHash.of("$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMN"),
                Roles.of(Role.CUSTOMER, Role.ADMIN),
                status, CREATED_AT, UPDATED_AT);
    }

    @Nested
    @DisplayName("save() — new entity")
    class SaveNew {

        @Test
        @DisplayName("persists a new UserEntity and returns the reconstituted domain User")
        void persistsNewEntity() {
            User user = newUser(UserStatus.PENDING);
            UserEntity savedEntity = new UserEntity(
                    user.idValue(), user.email().value(), user.passwordHash().value(),
                    user.roles().values(), user.status(), user.createdAt(), user.updatedAt());
            when(jpaRepository.findById(user.idValue())).thenReturn(Optional.empty());
            when(jpaRepository.save(any(UserEntity.class))).thenReturn(savedEntity);

            User result = adapter.save(user);

            assertThat(result.idValue()).isEqualTo(user.idValue());
            assertThat(result.email()).isEqualTo(user.email());
            assertThat(result.passwordHash()).isEqualTo(user.passwordHash());
            assertThat(result.roles()).isEqualTo(user.roles());
            assertThat(result.status()).isEqualTo(user.status());
            verify(jpaRepository).save(any(UserEntity.class));
        }
    }

    @Nested
    @DisplayName("save() — existing entity")
    class SaveExisting {

        @Test
        @DisplayName("updates fields on the existing entity (preserving @Version)")
        void updatesExistingEntity() {
            User domain = newUser(UserStatus.ACTIVE);
            UserEntity existing = new UserEntity(
                    domain.idValue(), domain.email().value(), "old-hash",
                    Set.of(Role.CUSTOMER), UserStatus.PENDING, CREATED_AT, CREATED_AT);

            when(jpaRepository.findById(domain.idValue())).thenReturn(Optional.of(existing));
            when(jpaRepository.save(existing)).thenReturn(existing);

            adapter.save(domain);

            assertThat(existing.getPasswordHash()).isEqualTo(domain.passwordHash().value());
            assertThat(existing.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(existing.getRoles()).containsExactlyInAnyOrder(Role.CUSTOMER, Role.ADMIN);
            assertThat(existing.getUpdatedAt()).isEqualTo(UPDATED_AT);
        }
    }

    @Nested
    @DisplayName("findById / findByEmail / existsByEmail")
    class ReadOperations {

        @Test
        @DisplayName("findById maps entity to domain User")
        void findByIdMapsToDomain() {
            UUID id = UUID.randomUUID();
            UserEntity entity = new UserEntity(
                    id, "alice@example.com",
                    "$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMN",
                    Set.of(Role.CUSTOMER), UserStatus.ACTIVE, CREATED_AT, UPDATED_AT);
            when(jpaRepository.findById(id)).thenReturn(Optional.of(entity));

            Optional<User> result = adapter.findById(UserId.of(id));

            assertThat(result).isPresent();
            assertThat(result.get().email().value()).isEqualTo("alice@example.com");
            assertThat(result.get().status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(result.get().roles().values()).containsExactly(Role.CUSTOMER);
        }

        @Test
        @DisplayName("findById returns empty when JPA returns empty")
        void findByIdEmpty() {
            UUID id = UUID.randomUUID();
            when(jpaRepository.findById(id)).thenReturn(Optional.empty());

            assertThat(adapter.findById(UserId.of(id))).isEmpty();
        }

        @Test
        @DisplayName("findByEmail delegates with the raw email string")
        void findByEmailDelegates() {
            UserEntity entity = new UserEntity(
                    UUID.randomUUID(), "alice@example.com",
                    "$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMN",
                    Set.of(Role.ADMIN), UserStatus.ACTIVE, CREATED_AT, UPDATED_AT);
            when(jpaRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(entity));

            Optional<User> result = adapter.findByEmail(Email.of("alice@example.com"));

            assertThat(result).isPresent();
            assertThat(result.get().roles().values()).containsExactly(Role.ADMIN);
        }

        @Test
        @DisplayName("existsByEmail delegates with the raw email string")
        void existsByEmailDelegates() {
            when(jpaRepository.existsByEmail("alice@example.com")).thenReturn(true);

            assertThat(adapter.existsByEmail(Email.of("alice@example.com"))).isTrue();
            verify(jpaRepository, never()).save(any());
        }
    }
}