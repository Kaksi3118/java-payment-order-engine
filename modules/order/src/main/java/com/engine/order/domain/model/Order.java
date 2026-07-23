package com.engine.order.domain.model;

import com.engine.shared.domain.model.AggregateRoot;
import com.engine.shared.domain.event.DomainEvent;
import com.engine.shared.domain.ids.OrderId;
import com.engine.shared.domain.ids.UserId;
import com.engine.shared.domain.model.Money;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Order aggregate root.
 *
 * <p>The transactional consistency boundary for one customer's order: its line items,
 * status, and total amount. State transitions happen only through methods on this
 * aggregate, each raising a {@link DomainEvent} that the application layer drains to
 * the transactional outbox.
 *
 * <p><strong>Invariants enforced:</strong>
 * <ul>
 *     <li>All {@link OrderItem}s must share the same currency (single-currency order).</li>
 *     <li>At least one item is required (empty orders are rejected).</li>
 *     <li>Status transitions follow the {@link OrderStatus} state machine.</li>
 *     <li>Total amount is derived from items, never set externally.</li>
 * </ul>
 */
public final class Order extends AggregateRoot {

    private final OrderId id;
    private final UserId customerId;
    private final List<OrderItem> items;
    private final Currency currency;
    private final Instant createdAt;
    private OrderStatus status;
    private Instant updatedAt;

    private Order(OrderId id, UserId customerId, List<OrderItem> items, Currency currency,
                  OrderStatus status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "OrderId must not be null");
        this.customerId = Objects.requireNonNull(customerId, "UserId must not be null");
        this.items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * Factory: a freshly placed order. Status begins at {@link OrderStatus#CREATED}.
     * Validates single-currency invariant and non-empty items.
     */
    public static Order place(UserId customerId, List<OrderItem> items, Clock clock) {
        Objects.requireNonNull(items, "items must not be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        Currency currency = items.get(0).unitPrice().currency();
        for (OrderItem item : items) {
            if (!item.unitPrice().currency().equals(currency)) {
                throw new com.engine.shared.domain.model.CurrencyMismatchException(
                        currency, item.unitPrice().currency());
            }
        }

        Instant now = Instant.now(Objects.requireNonNull(clock, "Clock must not be null"));
        OrderId orderId = OrderId.random();
        Order order = new Order(orderId, customerId, new ArrayList<>(items), currency,
                OrderStatus.CREATED, now, now);
        order.raise(new com.engine.order.domain.event.OrderPlaced(
                orderId.value(), now, orderId.value(), customerId.value(),
                com.engine.order.domain.event.OrderPlaced.toSnapshot(items)));
        return order;
    }

    /**
     * Reconstitution factory: rebuilds an Order from persisted state without raising events.
     */
    public static Order reconstitute(OrderId id, UserId customerId, List<OrderItem> items,
                                     Currency currency, OrderStatus status,
                                     Instant createdAt, Instant updatedAt) {
        return new Order(id, customerId, items, currency, status, createdAt, updatedAt);
    }

    public void confirm(Clock clock) {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Only a CREATED order can be confirmed; current status: " + status);
        }
        status = OrderStatus.CONFIRMED;
        updatedAt = Instant.now(clock);
        raise(new com.engine.order.domain.event.OrderConfirmed(
                id.value(), updatedAt, id.value()));
    }

    public void ship(Clock clock) {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only a CONFIRMED order can be shipped; current status: " + status);
        }
        status = OrderStatus.SHIPPED;
        updatedAt = Instant.now(clock);
    }

    public void deliver(Clock clock) {
        if (status != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Only a SHIPPED order can be delivered; current status: " + status);
        }
        status = OrderStatus.DELIVERED;
        updatedAt = Instant.now(clock);
    }

    public void cancel(Clock clock) {
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel an order that has been shipped or delivered; current status: " + status);
        }
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already CANCELLED");
        }
        status = OrderStatus.CANCELLED;
        updatedAt = Instant.now(clock);
        raise(new com.engine.order.domain.event.OrderCancelled(
                id.value(), updatedAt, id.value(), customerId.value()));
    }

    public Money totalAmount() {
        Money total = Money.zero(currency);
        for (OrderItem item : items) {
            total = total.plus(item.lineTotal());
        }
        return total;
    }

    public OrderId id() { return id; }
    public java.util.UUID idValue() { return id.value(); }
    public UserId customerId() { return customerId; }
    public java.util.UUID customerIdValue() { return customerId.value(); }
    public List<OrderItem> items() { return items; }
    public Currency currency() { return currency; }
    public OrderStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public OrderView toView() {
        return new OrderView(id.value(), customerId.value(), status, currency,
                items, totalAmount(), createdAt, updatedAt);
    }
}