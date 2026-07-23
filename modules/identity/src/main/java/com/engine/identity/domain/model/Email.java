package com.engine.identity.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validated, normalized email value object.
 *
 * <p>Validation is intentionally a permissive RFC-5322 <em>subset</em> ({@code local@domain.tld}),
 * not the full grammar — the canonical authority for email validity is delivery, not a regex.
 * The local-part is case-insensitive in practice; we normalize to lowercase so {@link #equals}
 * is stable across input forms.
 */
public record Email(String value) implements Comparable<Email> {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        Objects.requireNonNull(value, "email value must not be null");
        String trimmed = value.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
        value = trimmed.toLowerCase();
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public int compareTo(Email other) {
        return value.compareTo(other.value);
    }
}