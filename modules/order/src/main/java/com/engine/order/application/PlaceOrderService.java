package com.engine.order.application;

import com.engine.order.domain.model.Order;
import com.engine.order.domain.port.in.PlaceOrderUseCase;
import com.engine.order.domain.port.out.IdempotencyPort;
import com.engine.order.domain.port.out.InventoryPort;
import com.engine.order.domain.port.out.OrderRepository;
import com.engine.shared.domain.event.DomainEvent;
import com.engine.shared.domain.port.out.EventOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/**
 * Use case: place a new order.
 *
 * <p>Orchestrates in a single transaction:
 * <ol>
 *     <li><strong>Idempotency guard</strong> &mdash; check {@link IdempotencyPort#findResult}
 *         for a cached result. If found, return it immediately without re-executing.</li>
 *     <li><strong>Inventory reservation</strong> &mdash; call {@link InventoryPort#reserve}
 *         to atomically reserve all line items. If any product is unavailable, the
 *         {@code InsufficientInventoryException} propagates and the order is never saved.</li>
 *     <li><strong>Order creation</strong> &mdash; {@link Order#place} creates the aggregate
 *         (CREATED status, raises {@code OrderPlaced}).</li>
 *     <li><strong>Persist</strong> &mdash; save the aggregate via {@link OrderRepository#save}.</li>
 *     <li><strong>Outbox drain</strong> &mdash; drain domain events to {@link EventOutbox#append}
 *         in the same transaction, then clear the aggregate's event buffer.</li>
 *     <li><strong>Idempotency save</strong> &mdash; persist the result via
 *         {@link IdempotencyPort#saveResult} so future replays return the cached value.</li>
 * </ol>
 *
 * <p>If any step fails, the entire transaction rolls back &mdash; including the outbox rows
 * and the idempotency entry, so no side effect survives a failure.
 */
@Service
public class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderRepository orderRepository;
    private final InventoryPort inventoryPort;
    private final IdempotencyPort idempotencyPort;
    private final EventOutbox eventOutbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PlaceOrderService(OrderRepository orderRepository,
                             InventoryPort inventoryPort,
                             IdempotencyPort idempotencyPort,
                             EventOutbox eventOutbox,
                             ObjectMapper objectMapper,
                             Clock clock) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "OrderRepository must not be null");
        this.inventoryPort = Objects.requireNonNull(inventoryPort, "InventoryPort must not be null");
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "IdempotencyPort must not be null");
        this.eventOutbox = Objects.requireNonNull(eventOutbox, "EventOutbox must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "ObjectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
    }

    @Override
    @Transactional
    public PlaceOrderResult place(PlaceOrderCommand command) {
        Optional<String> cached = idempotencyPort.findResult(command.idempotencyKey(), command.requestHash());
        if (cached.isPresent()) {
            return deserializeResult(cached.get());
        }

        Order order = Order.place(command.customerId(), command.items(), clock);

        inventoryPort.reserve(order.idValue(), order.items());

        orderRepository.save(order);

        for (DomainEvent event : order.domainEvents()) {
            eventOutbox.append(event);
        }
        order.clearEvents();

        PlaceOrderResult result = new PlaceOrderResult(order.idValue());
        idempotencyPort.saveResult(command.idempotencyKey(), command.requestHash(),
                serializeResult(result));

        return result;
    }

    private String serializeResult(PlaceOrderResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize PlaceOrderResult", e);
        }
    }

    private PlaceOrderResult deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, PlaceOrderResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize PlaceOrderResult", e);
        }
    }
}