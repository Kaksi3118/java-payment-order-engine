package com.engine.identity.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Email value object")
class EmailTest {

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("accepts a well-formed email and normalizes to lowercase")
        void acceptsAndNormalizes() {
            Email email = new Email("John.Doe@Example.COM");
            assertThat(email.value()).isEqualTo("john.doe@example.com");
        }

        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            assertThatThrownBy(() -> new Email(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects missing @")
        void rejectsMissingAt() {
            assertThatThrownBy(() -> new Email("not-an-email"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects missing domain")
        void rejectsMissingDomain() {
            assertThatThrownBy(() -> new Email("user@"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects missing TLD")
        void rejectsMissingTld() {
            assertThatThrownBy(() -> new Email("user@localhost"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects whitespace inside the email")
        void rejectsInternalWhitespace() {
            assertThatThrownBy(() -> new Email("user @example.com"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("equals is case-insensitive via normalization")
    void equalsAfterNormalization() {
        Email a = Email.of("Alice@Example.com");
        Email b = Email.of("alice@example.com");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("trims surrounding whitespace before validation")
    void trimsSurroundingWhitespace() {
        Email email = Email.of("  alice@example.com  ");
        assertThat(email.value()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("compareTo is consistent with value ordering")
    void compareTo() {
        Email lower = Email.of("alice@example.com");
        Email upper = Email.of("bob@example.com");
        assertThat(lower).isLessThan(upper);
        assertThat(upper).isGreaterThan(lower);
    }
}