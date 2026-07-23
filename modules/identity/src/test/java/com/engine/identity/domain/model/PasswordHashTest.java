package com.engine.identity.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PasswordHash value object")
class PasswordHashTest {

    @Test
    @DisplayName("accepts a valid BCrypt hash form")
    void acceptsValidBcryptHash() {
        PasswordHash hash = PasswordHash.of("$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMN");
        assertThat(hash.value()).startsWith("$2");
    }

    @Test
    @DisplayName("rejects null")
    void rejectsNull() {
        assertThatThrownBy(() -> new PasswordHash(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("rejects a blank hash")
    void rejectsBlank() {
        assertThatThrownBy(() -> new PasswordHash("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects a non-BCrypt hash on construction (defensive invariant)")
    void rejectsNonBcryptFormat() {
        assertThatThrownBy(() -> new PasswordHash("argon2id$v=19$m=65536"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BCrypt");
    }
}