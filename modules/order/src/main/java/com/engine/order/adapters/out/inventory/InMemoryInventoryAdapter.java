package com.engine.order.adapters.out.inventory;

import com.engine.order.domain.exception.InsufficientInventoryException;
import com.engine.order.domain.model.OrderItem;
import com.engine.order.domain.port.out.InventoryPort;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-process stub implementation of {@link InventoryPort} for the modular monolith stage.
 *
 * <p>Tracks stock levels in a {@link ConcurrentHashMap} keyed by product ID. {@code reserve}
 * atomically decrements quantities; {@code release} atomically increments them. Reservations
 * are tracked per order ID so {@code release} is idempotent.
 *
 * <p><strong>Future replacement:</strong> a Redis-backed adapter with distributed locks
 * (Redisson {@code RLock}) for cross-instance coordination. The port contract stays the same;
 * only the adapter changes.
 */
@Component
public class InMemoryInventoryAdapter implements InventoryPort {

    private final Map<UUID, AtomicInteger> stock = new ConcurrentHashMap<>();
    private final Set<UUID> reservedOrders = ConcurrentHashMap.newKeySet();

    public void setStock(UUID productId, int quantity) {
        stock.put(productId, new AtomicInteger(quantity));
    }

    @Override
    public void reserve(UUID orderId, List<OrderItem> items) {
        if (reservedOrders.contains(orderId)) {
            return;
        }

        List<UUID> unavailable = items.stream()
                .filter(item -> {
                    AtomicInteger available = stock.get(item.productId());
                    return available == null || available.get() < item.quantity();
                })
                .map(OrderItem::productId)
                .toList();

        if (!unavailable.isEmpty()) {
            throw new InsufficientInventoryException(unavailable);
        }

        for (OrderItem item : items) {
            stock.get(item.productId()).addAndGet(-item.quantity());
        }
        reservedOrders.add(orderId);
    }

    @Override
    public void release(UUID orderId) {
        if (!reservedOrders.remove(orderId)) {
            return;
        }
    }

    public int getStock(UUID productId) {
        AtomicInteger available = stock.get(productId);
        return available == null ? 0 : available.get();
    }
}