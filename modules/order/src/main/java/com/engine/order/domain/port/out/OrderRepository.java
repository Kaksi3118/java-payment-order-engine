package com.engine.order.domain.port.out;

import com.engine.order.domain.model.Order;
import com.engine.shared.domain.ids.OrderId;

import java.util.Optional;

/**
 * Driven port: persistence of the {@link Order} aggregate.
 *
 * <p>Live adapters live in {@code adapters.out} (Spring Data JPA). The adapter must
 * preserve JPA's {@code @Version} for optimistic locking &mdash; load-then-update, not
 * naive merge.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(OrderId id);
}