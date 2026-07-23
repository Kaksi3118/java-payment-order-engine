package com.engine.order.application;

import com.engine.order.domain.exception.OrderNotFoundException;
import com.engine.order.domain.model.Order;
import com.engine.order.domain.port.in.CancelOrderUseCase;
import com.engine.order.domain.port.out.IdempotencyPort;
import com.engine.order.domain.port.out.InventoryPort;
import com.engine.order.domain.port.out.OrderRepository;
import com.engine.shared.domain.event.DomainEvent;
import com.engine.shared.domain.port.out.EventOutbox;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/**
 * Use case: cancel an existing order.
 *
 * <p>Orchestrates in a single transaction:
 * <ol>
 *     <li><strong>Idempotency guard</strong> &mdash; same as {@link PlaceOrderService}.</li>
 *     <li><strong>Load</strong> the order by ID; throw {@link OrderNotFoundException} if absent.</li>
 *     <li><strong>Cancel</strong> &mdash; {@link Order#cancel} transitions to CANCELLED and
 *         raises {@code OrderCancelled}.</li>
 *     <li><strong>Release inventory</strong> &mdash; {@link InventoryPort#release} frees the
 *         reserved stock. Idempotent at the port level.</li>
 *     <li><strong>Persist</strong> the cancelled aggregate.</li>
 *     <li><strong>Outbox drain</strong> + idempotency save (same as place).</li>
 * </ol>
 */
@Service
public class CancelOrderService implements CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final InventoryPort inventoryPort;
    private final IdempotencyPort idempotencyPort;
    private final EventOutbox eventOutbox;
    private final Clock clock;

    public CancelOrderService(OrderRepository orderRepository,
                              InventoryPort inventoryPort,
                              IdempotencyPort idempotencyPort,
                              EventOutbox eventOutbox,
                              Clock clock) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "OrderRepository must not be null");
        this.inventoryPort = Objects.requireNonNull(inventoryPort, "InventoryPort must not be null");
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "IdempotencyPort must not be null");
        this.eventOutbox = Objects.requireNonNull(eventOutbox, "EventOutbox must not be null");
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
    }

    @Override
    @Transactional
    public void cancel(CancelOrderCommand command) {
        Optional<String> cached = idempotencyPort.findResult(command.idempotencyKey(), command.requestHash());
        if (cached.isPresent()) {
            return;
        }

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId().value()));

        order.cancel(clock);
        inventoryPort.release(order.idValue());
        orderRepository.save(order);

        for (DomainEvent event : order.domainEvents()) {
            eventOutbox.append(event);
        }
        order.clearEvents();

        idempotencyPort.saveResult(command.idempotencyKey(), command.requestHash(), "{\"cancelled\":true}");
    }
}