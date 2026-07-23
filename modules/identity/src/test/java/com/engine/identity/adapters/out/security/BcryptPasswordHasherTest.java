package com.engine.identity.adapters.out.security;

import com.engine.identity.domain.model.PasswordHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

@DisplayName("BcryptPasswordHasher")
class BcryptPasswordHasherTest {

    private final BcryptPasswordHasher hasher = new BcryptPasswordHasher();

    @Test
    @DisplayName("hash returns a BCrypt-form hash that is NOT byte-equal to the raw password")
    void hashProducesBcryptForm() {
        PasswordHash hash = hasher.hash("p@ssw0rd");

        assertThat(hash.value()).startsWith("$2");
        assertThat(hash.value()).isNotEqualTo("p@ssw0rd");
        assertThat(hash.value()).hasSize(60);
    }

    @Test
    @DisplayName("matches: returns true for the raw password that was hashed; false for any other")
    void matchesVerifiesRawPassword() {
        PasswordHash hash = hasher.hash("p@ssw0rd");

        assertThat(hasher.matches("p@ssw0rd", hash)).isTrue();
        assertThat(hasher.matches("wr0ng", hash)).isFalse();
        assertThat(hasher.matches("", hash)).isFalse();
    }

    @Test
    @DisplayName("hashing the same raw password twice produces distinct hashes (salt is randomized)")
    void sameRawProducesDifferentHashes() {
        PasswordHash a = hasher.hash("p@ssw0rd");
        PasswordHash b = hasher.hash("p@ssw0rd");

        assertThat(a).isNotEqualTo(b);
        assertThat(hasher.matches("p@ssw0rd", a)).isTrue();
        assertThat(hasher.matches("p@ssw0rd", b)).isTrue();
    }

    @Test
    @DisplayName("rejects null inputs with NullPointerException")
    void rejectsNulls() {
        assertThatNullPointerException().isThrownBy(() -> hasher.hash(null));

        PasswordHash validHash = hasher.hash("p@ssw0rd");
        assertThatNullPointerException().isThrownBy(() -> hasher.matches(null, validHash));
        assertThatNullPointerException().isThrownBy(() -> hasher.matches("p", null));
    }
}