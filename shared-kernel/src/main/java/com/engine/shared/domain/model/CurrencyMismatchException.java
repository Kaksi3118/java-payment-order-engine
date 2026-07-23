package com.engine.shared.domain.model;

import java.util.Currency;

/**
 * Raised when an operation is attempted across two {@link Money} instances that
 * are denominated in different currencies. Money is single-currency by design;
 * currency conversion is a domain service, not an arithmetic primitive.
 */
public final class CurrencyMismatchException extends RuntimeException {

    private final Currency expected;
    private final Currency actual;

    public CurrencyMismatchException(Currency expected, Currency actual) {
        super("Currency mismatch: expected " + expected.getCurrencyCode()
                + " but received " + actual.getCurrencyCode());
        this.expected = expected;
        this.actual = actual;
    }

    public Currency expected() {
        return expected;
    }

    public Currency actual() {
        return actual;
    }
}