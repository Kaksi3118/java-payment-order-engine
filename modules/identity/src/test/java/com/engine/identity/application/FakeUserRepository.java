package com.engine.identity.application;

import com.engine.identity.domain.model.Email;
import com.engine.identity.domain.model.User;
import com.engine.identity.domain.port.out.UserRepository;
import com.engine.shared.domain.ids.UserId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fake of {@link UserRepository} for unit tests.
 *
 * <p>Hand-written rather than Mockito-mocked because the register use case calls
 * {@code existsByEmail} then {@code save} &mdash; a stateful sequence that a mock cannot
 * emulate without brittle {@code when(...)} stubs. The fake maintains real state, so the
 * sequence behaves exactly as a real repository would.
 */
final class FakeUserRepository implements UserRepository {

    private final Map<UserId, User> byId = new ConcurrentHashMap<>();
    private final Map<String, User> byEmail = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        Objects.requireNonNull(user);
        byId.put(user.id(), user);
        byEmail.put(user.email().value(), user);
        return user;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return Optional.ofNullable(byEmail.get(email.value()));
    }

    @Override
    public boolean existsByEmail(Email email) {
        return byEmail.containsKey(email.value());
    }

    int size() {
        return byId.size();
    }
}