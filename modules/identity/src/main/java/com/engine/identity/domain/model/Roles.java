package com.engine.identity.domain.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable, value-semantic set of {@link Role}s owned by a {@link User}.
 *
 * <p>Wraps a {@link LinkedHashSet} so iteration order is deterministic &mdash; important when
 * serializing roles into a JWT claim that consumer code may diff across tokens.
 */
public record Roles(Set<Role> values) {

    public Roles {
        Objects.requireNonNull(values, "roles must not be null");
        values = Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    public static Roles of(Role... roles) {
        Objects.requireNonNull(roles, "roles varargs must not be null");
        return new Roles(new LinkedHashSet<>(Arrays.asList(roles)));
    }

    public static Roles empty() {
        return new Roles(Set.of());
    }

    public static Roles from(Collection<Role> roles) {
        Objects.requireNonNull(roles, "roles collection must not be null");
        return new Roles(new LinkedHashSet<>(roles));
    }

    public boolean contains(Role role) {
        return values.contains(role);
    }

    public boolean containsAny(Role... candidates) {
        for (Role candidate : candidates) {
            if (values.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsAll(Role... required) {
        for (Role role : required) {
            if (!values.contains(role)) {
                return false;
            }
        }
        return true;
    }

    public Roles union(Roles other) {
        Set<Role> merged = new LinkedHashSet<>(values);
        merged.addAll(other.values);
        return new Roles(merged);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public Set<Role> values() {
        return Collections.unmodifiableSet(values);
    }
}