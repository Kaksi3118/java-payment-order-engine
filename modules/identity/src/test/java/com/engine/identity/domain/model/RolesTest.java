package com.engine.identity.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Roles value object")
class RolesTest {

    @Test
    @DisplayName("of() preserves insertion order via immutable copy")
    void ofPreservesOrderAndIsImmutable() {
        Roles roles = Roles.of(Role.ADMIN, Role.CUSTOMER);
        assertThat(roles.values()).containsExactly(Role.ADMIN, Role.CUSTOMER);
        assertThatThrownBy(() -> roles.values().add(Role.SUPPORT))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("contains / containsAny / containsAll")
    void membershipQueries() {
        Roles roles = Roles.of(Role.ADMIN);
        assertThat(roles.contains(Role.ADMIN)).isTrue();
        assertThat(roles.contains(Role.CUSTOMER)).isFalse();
        assertThat(roles.containsAny(Role.ADMIN, Role.CUSTOMER)).isTrue();
        assertThat(roles.containsAny(Role.SUPPORT, Role.CUSTOMER)).isFalse();
        assertThat(roles.containsAll(Role.ADMIN)).isTrue();
        assertThat(roles.containsAll(Role.ADMIN, Role.CUSTOMER)).isFalse();
    }

    @Test
    @DisplayName("union returns a new Roles value object")
    void unionReturnsNewInstance() {
        Roles a = Roles.of(Role.ADMIN);
        Roles b = Roles.of(Role.SUPPORT);
        Roles merged = a.union(b);
        assertThat(merged.values()).containsExactlyInAnyOrder(Role.ADMIN, Role.SUPPORT);
        assertThat(a.values()).containsExactly(Role.ADMIN);
        assertThat(b.values()).containsExactly(Role.SUPPORT);
    }

    @Test
    @DisplayName("empty factory yields an empty (immutable) Roles")
    void emptyFactory() {
        Roles empty = Roles.empty();
        assertThat(empty.isEmpty()).isTrue();
        assertThat(empty.values()).isEmpty();
    }

    @Test
    @DisplayName("rejects null varargs array and null collection")
    void rejectsNulls() {
        assertThatThrownBy(() -> Roles.of((Role[]) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Roles.from(null))
                .isInstanceOf(NullPointerException.class);
    }
}