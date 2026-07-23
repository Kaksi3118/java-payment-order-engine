package com.engine.shared.domain.model;

import java.util.UUID;

/**
 * Marker contract for strongly-typed aggregate identifiers.
 *
 * <p>Avoids primitive obsession: a method {@code reserve(OrderId, Money)}
 * cannot accept a {@code UserId} by mistake, whereas {@code reserve(UUID, Money)}
 * could. All concrete identifiers expose their underlying {@link UUID} via
 * {@link #value()} so that the application and adapter layers can uniformly
 * serialize, index, and correlate entities.
 */
public interface Identifier {
    UUID value();
}