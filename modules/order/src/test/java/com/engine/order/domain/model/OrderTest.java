package com.engine.order.domain.model;

import com.engine.order.domain.event.OrderCancelled;
import com.engine.order.domain.event.OrderConfirmed;
import com.engine.order.domain.event.OrderPlaced;
import com.engine.shared.domain.ids.UserId;
import com.engine.shared.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order aggregate")
class OrderTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC);
    private static final Currency USD = Currency.getInstance("USD");
    private static final UUID PRODUCT_A = UUID.randomUUID();
    private static final UUID PRODUCT_B = UUID.randomUUID();

    private static OrderItem item(UUID product, int qty, String price) {
        return new OrderItem(product, qty, Money.of(new BigDecimal(price), USD));
    }

    private static List<OrderItem> defaultItems() {
        return List.of(item(PRODUCT_A, 2, "10.00"), item(PRODUCT_B, 1, "25.50"));
    }

    @Nested
    @DisplayName("place()")
    class PlaceFactory {

        @Test
        @DisplayName("starts in CREATED and raises OrderPlaced event")
        void startsCreatedWithEvent() {
            Order order = Order.place(UserId.random(), defaultItems(), FIXED_CLOCK);

            assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
            assertThat(order.items()).hasSize(2);
            assertThat(order.currency()).isEqualTo(USD);
            assertThat(order.createdAt()).isEqualTo(Instant.parse("2026-07-23T12:00:00Z"));

            assertThat(order.domainEvents())
                    .singleElement()
                    .isInstanceOf(OrderPlaced.class);

            OrderPlaced event = (OrderPlaced) order.domainEvents().get(0);
            assertThat(event.aggregateId()).isEqualTo(order.idValue());
            assertThat(event.items()).hasSize(2);
            assertThat(event.items().get(0).productId()).isEqualTo(PRODUCT_A);
            assertThat(event.items().get(0).quantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("total amount is derived from items")
        void totalAmountDerived() {
            Order order = Order.place(UserId.random(), defaultItems(), FIXED_CLOCK);
            // 2 * 10.00 + 1 * 25.50 = 45.50
            assertThat(order.totalAmount()).isEqualTo(Money.of(new BigDecimal("45.50"), USD));
        }

        @Test
        @DisplayName("rejects empty items list")
        void rejectsEmptyItems() {
            assertThatThrownBy(() -> Order.place(UserId.random(), List.of(), FIXED_CLOCK))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one item");
        }

        @Test
        @DisplayName("rejects mixed-currency items")
        void rejectsMixedCurrency() {
            OrderItem usdItem = new OrderItem(PRODUCT_A, 1, Money.of(new BigDecimal("10"), USD));
            OrderItem eurItem = new OrderItem(PRODUCT_B, 1,
                    Money.of(new BigDecimal("10"), Currency.getInstance("EUR")));

            assertThatThrownBy(() -> Order.place(UserId.random(), List.of(usdItem, eurItem), FIXED_CLOCK))
                    .isInstanceOf(com.engine.shared.domain.model.CurrencyMismatchException.class);
        }
    }

    @Nested
    @DisplayName("state transitions")
    class Transitions {

        @Test
        @DisplayName("confirm: CREATED -> CONFIRMED raises OrderConfirmed")
        void createdToConfirmed() {
            Order order = Order.place(UserId.random(), defaultItems(), FIXED_CLOCK);
            order.confirm(FIXED_CLOCK);

            assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(order.domainEvents()).hasSize(2);
            assertThat(order.domainEvents().get(1)).isInstanceOf(OrderConfirmed.class);
        }

        @Test
        @DisplayName("ship: CONFIRMED -> SHIPPED")
        void confirmedToShipped() {
            Order order = Order.place(UserId.random(), defaultItems(), FIXED_CLOCK);
            order.confirm(FIXED_CLOCK);
            order.ship(FIXED_CLOCK);
            assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
        }

        @Test
        @DisplayName("deliver: SHIPPED -> DELIVERED (terminal)")
        void shippedToDelivered() {
            Order order = Order.place(UserId.random(), defaultItems(), FIXED_CLOCK);
            order.confirm(FIXED_CLOCK);
            order.ship(FIXED_CLOCK);
            order.deliver(FIXED_CLOCK);
            assertThat(order.status()).isEqualTo(OrderStatus.DELIVERED);
        }

        @Test
        @DisplayName("confirm: rejects non-CREATED order")
        void confirmRequiresCreated() {
            Order order = Order.place(UserId.random(), defaultItems(), FIXED_CLOCK);
            order.confirm(FIXED_CLOCK);
            assertThatThrownBy(() -> order.confirm(FIXED_CLOCK))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("ship: rejects non-CONFIRMED order")
        void shipRequiresConfirmed() {
            Order order = Order.place(UserId.random(), defaultItems(), FIXED_CLOCK);
            assertThatThrownBy(() -> order.ship(FIXED_CLOCK))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("deliver: rejects non-SHIPPED order")
        void deliverRequiresShipped() {
            Order order = Order.place(UserId.random(), defaultItems(), FIXED_CLOCK);
            order.confirm(FIXED_CLOCK);
            assertThatThrownBy(() -> order.deliver(FIXED_CLOCK))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("cancel from CREATED raises OrderCancelled")
        void cancelFromCreated() {
            Order order = Order.place(UserId.random(), defaultItems(), FIXED_CLOCK);
            order.cancel(FIXED_CLOCK);

            assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.domainEvents().get(1)).isInstanceOf(OrderCancelled.class);

            OrderCancelled event = (OrderCancelled) order.domainEvents().get(1);
            assertThat(event.customerId()).isEqualTo(order.customerIdValue());
        }

        @Test
        @DisplayName("cancel from CONFIRMED is allowed")
        void cancelFromConfirmed() {
            Order order = Order.place(UserId.random(), defaultItems(), FIXED_CLOCK);
            order.confirm(FIXED_CLOCK);
            order.cancel(FIXED_CLOCK);
            assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("cancel from SHIPPED is rejected")
        void cancelFromShippedRejected() {
            Order order = Order.place(UserId.random(), defaultItems(), FIXED_CLOCK);
            order.confirm(FIXED_CLOCK);
            order.ship(FIXED_CLOCK);
            assertThatThrownBy(() -> order.cancel(FIXED_CLOCK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("shipped");
        }

        @Test
        @DisplayName("cancel from DELIVERED is rejected")
        void cancelFromDeliveredRejected() {
            Order order = Order.place(UserId.random(), defaultItems(), FIXED_CLOCK);
            order.confirm(FIXED_CLOCK);
            order.ship(FIXED_CLOCK);
            order.deliver(FIXED_CLOCK);
            assertThatThrownBy(() -> order.cancel(FIXED_CLOCK))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("double cancel is rejected")
        void doubleCancelRejected() {
            Order order = Order.place(UserId.random(), defaultItems(), FIXED_CLOCK);
            order.cancel(FIXED_CLOCK);
            assertThatThrownBy(() -> order.cancel(FIXED_CLOCK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already CANCELLED");
        }
    }

    @Nested
    @DisplayName("reconstitute()")
    class Reconstitute {

        @Test
        @DisplayName("reconstitutes without raising events")
        void reconstitutesWithoutEvents() {
            Order order = Order.reconstitute(
                    com.engine.shared.domain.ids.OrderId.random(),
                    UserId.random(),
                    defaultItems(),
                    USD,
                    OrderStatus.CONFIRMED,
                    Instant.parse("2026-07-23T12:00:00Z"),
                    Instant.parse("2026-07-23T12:30:00Z"));

            assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(order.domainEvents()).isEmpty();
        }
    }

    @Test
    @DisplayName("toView produces a consistent snapshot")
    void toViewSnapshot() {
        Order order = Order.place(UserId.random(), defaultItems(), FIXED_CLOCK);
        OrderView view = order.toView();

        assertThat(view.orderId()).isEqualTo(order.idValue());
        assertThat(view.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(view.currency()).isEqualTo(USD);
        assertThat(view.items()).hasSize(2);
        assertThat(view.totalAmount()).isEqualTo(Money.of(new BigDecimal("45.50"), USD));
    }

    @Test
    @DisplayName("OrderItem rejects zero or negative quantity")
    void orderItemRejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> new OrderItem(PRODUCT_A, 0, Money.of(new BigDecimal("10"), USD)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderItem(PRODUCT_A, -1, Money.of(new BigDecimal("10"), USD)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("OrderItem.lineTotal = unitPrice * quantity")
    void orderItemLineTotal() {
        OrderItem item = new OrderItem(PRODUCT_A, 3, Money.of(new BigDecimal("10.00"), USD));
        assertThat(item.lineTotal()).isEqualTo(Money.of(new BigDecimal("30.00"), USD));
    }
}