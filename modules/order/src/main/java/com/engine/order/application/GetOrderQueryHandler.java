package com.engine.order.application;

import com.engine.order.domain.model.OrderView;
import com.engine.order.domain.port.in.GetOrderQuery;
import com.engine.order.domain.port.out.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/**
 * Query handler (CQRS read side): retrieve an order by ID.
 *
 * <p>Read-only &mdash; {@code @Transactional(readOnly = true)}. Loads the {@link com.engine.order.domain.model.Order}
 * aggregate from the repository and maps it to an {@link OrderView} snapshot via
 * {@link com.engine.order.domain.model.Order#toView()}. Never raises domain events or writes
 * to the outbox.
 */
@Service
public class GetOrderQueryHandler implements GetOrderQuery {

    private final OrderRepository orderRepository;

    public GetOrderQueryHandler(OrderRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "OrderRepository must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderView> findById(com.engine.shared.domain.ids.OrderId orderId) {
        return orderRepository.findById(orderId).map(com.engine.order.domain.model.Order::toView);
    }
}