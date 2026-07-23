package com.engine.order.domain.port.out;

import com.engine.order.domain.model.OrderItem;

import java.util.List;
import java.util.UUID;

/**
 * Driven port: inventory reservation.
 *
 * <p>The domain says "reserve these items" without knowing whether the implementation is
 * an in-process inventory service or a remote one. This is the hexagonal seam for future
 * microservice extraction.
 *
 * <p>Implementations MUST be idempotent: reserving the same items for the same order ID
 * twice must not double-count. The adapter will use a Redis distributed lock or a
 * database-level reservation table to guarantee this.
 */
public interface InventoryPort {

    /**
     * Attempts to reserve all given items. If any product is unavailable, reserves
     * nothing (atomic all-or-nothing) and throws {@link com.engine.order.domain.exception.InsufficientInventoryException}.
     *
     * @param orderId the order being placed (used for idempotent reservation)
     * @param items   the line items to reserve
     * @throws com.engine.order.domain.exception.InsufficientInventoryException if any product is unavailable
     */
    void reserve(UUID orderId, List<OrderItem> items);

    /**
     * Releases the reservation for a cancelled order. Idempotent.
     *
     * @param orderId the order whose reservation should be released
     */
    void release(UUID orderId);
}