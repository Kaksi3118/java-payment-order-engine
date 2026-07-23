package com.engine.order.domain.port.in;

import com.engine.order.domain.model.OrderView;
import com.engine.shared.domain.ids.OrderId;

import java.util.Optional;

/**
 * Driving port (query contract, CQRS read side): retrieve an order by ID.
 *
 * <p>Read-only &mdash; never raises domain events or writes to the outbox. The query
 * handler returns an {@link OrderView} snapshot, not the live {@link com.engine.order.domain.model.Order}
 * aggregate, keeping the read path fast and decoupled from write-side invariants.
 */
public interface GetOrderQuery {

    Optional<OrderView> findById(OrderId orderId);
}