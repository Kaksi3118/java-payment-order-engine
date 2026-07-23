package com.engine.order.application;

import com.engine.order.domain.exception.InsufficientInventoryException;
import com.engine.order.domain.model.OrderItem;
import com.engine.order.domain.port.out.InventoryPort;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Fake {@link InventoryPort} for unit tests.
 *
 * <p>Tracks which products are "unavailable" (configured per test). {@code reserve} throws
 * {@link InsufficientInventoryException} if any item's product is in the unavailable set;
 * otherwise records the reservation. {@code release} removes the reservation.
 */
final class FakeInventoryPort implements InventoryPort {

    private final Set<UUID> unavailableProducts = new HashSet<>();
    private final Set<UUID> reservedOrders = new HashSet<>();

    void markUnavailable(UUID productId) {
        unavailableProducts.add(productId);
    }

    @Override
    public void reserve(UUID orderId, List<OrderItem> items) {
        List<UUID> unavailable = items.stream()
                .map(OrderItem::productId)
                .filter(unavailableProducts::contains)
                .toList();
        if (!unavailable.isEmpty()) {
            throw new InsufficientInventoryException(unavailable);
        }
        reservedOrders.add(orderId);
    }

    @Override
    public void release(UUID orderId) {
        reservedOrders.remove(orderId);
    }

    boolean isReserved(UUID orderId) {
        return reservedOrders.contains(orderId);
    }
}