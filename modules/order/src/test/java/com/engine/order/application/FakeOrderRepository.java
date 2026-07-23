package com.engine.order.application;

import com.engine.order.domain.model.Order;
import com.engine.order.domain.port.out.OrderRepository;
import com.engine.shared.domain.ids.OrderId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory fake of {@link OrderRepository} for unit tests.
 */
final class FakeOrderRepository implements OrderRepository {

    private final Map<OrderId, Order> store = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        Objects.requireNonNull(order);
        store.put(order.id(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(store.get(id));
    }

    int size() {
        return store.size();
    }
}