package com.engine.order.application;

import com.engine.order.domain.exception.OrderNotFoundException;
import com.engine.order.domain.event.OrderCancelled;
import com.engine.order.domain.model.Order;
import com.engine.order.domain.model.OrderItem;
import com.engine.order.domain.model.OrderStatus;
import com.engine.order.domain.port.in.CancelOrderUseCase.CancelOrderCommand;
import com.engine.order.domain.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import com.engine.order.domain.port.in.PlaceOrderUseCase.PlaceOrderResult;
import com.engine.shared.domain.ids.OrderId;
import com.engine.shared.domain.ids.UserId;
import com.engine.shared.domain.model.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CancelOrderService")
class CancelOrderServiceTest {

    private FakeOrderRepository orderRepository;
    private FakeInventoryPort inventoryPort;
    private FakeIdempotencyPort idempotencyPort;
    private FakeEventOutbox eventOutbox;
    private PlaceOrderService placeOrderService;
    private CancelOrderService cancelOrderService;

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC);
    private static final Currency USD = Currency.getInstance("USD");

    @BeforeEach
    void setUp() {
        orderRepository = new FakeOrderRepository();
        inventoryPort = new FakeInventoryPort();
        idempotencyPort = new FakeIdempotencyPort();
        eventOutbox = new FakeEventOutbox();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        placeOrderService = new PlaceOrderService(orderRepository, inventoryPort, idempotencyPort,
                eventOutbox, objectMapper, FIXED_CLOCK, meterRegistry);
        cancelOrderService = new CancelOrderService(orderRepository, inventoryPort, idempotencyPort,
                eventOutbox, FIXED_CLOCK);
    }

    private OrderId seedOrder() {
        PlaceOrderResult result = placeOrderService.place(new PlaceOrderCommand(
                UserId.random(),
                List.of(new OrderItem(UUID.randomUUID(), 1, Money.of(new BigDecimal("10.00"), USD))),
                "place-key-" + UUID.randomUUID(),
                "place-hash"));
        return OrderId.of(result.orderId());
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("cancels order, releases inventory, drains OrderCancelled to outbox")
        void cancelsOrderEndToEnd() {
            OrderId orderId = seedOrder();
            eventOutbox.clear();

            cancelOrderService.cancel(new CancelOrderCommand(orderId, "cancel-key-1", "cancel-hash"));

            Order order = orderRepository.findById(orderId).orElseThrow();
            assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(inventoryPort.isReserved(orderId.value())).isFalse();
            assertThat(eventOutbox.size()).isEqualTo(1);
            assertThat(eventOutbox.events().get(0)).isInstanceOf(OrderCancelled.class);
        }
    }

    @Nested
    @DisplayName("idempotency")
    class IdempotencyGuard {

        @Test
        @DisplayName("second cancel with same key + hash is a no-op")
        void replayIsNoOp() {
            OrderId orderId = seedOrder();
            CancelOrderCommand command = new CancelOrderCommand(orderId, "cancel-key-1", "cancel-hash");

            cancelOrderService.cancel(command);
            int outboxSizeAfterFirst = eventOutbox.size();
            cancelOrderService.cancel(command);

            assertThat(eventOutbox.size()).isEqualTo(outboxSizeAfterFirst);
        }
    }

    @Nested
    @DisplayName("failure cases")
    class Failures {

        @Test
        @DisplayName("cancelling a non-existent order throws OrderNotFoundException")
        void orderNotFound() {
            assertThatThrownBy(() -> cancelOrderService.cancel(
                    new CancelOrderCommand(OrderId.random(), "key", "hash")))
                    .isInstanceOf(OrderNotFoundException.class);
        }

        @Test
        @DisplayName("cancelling an already-cancelled order throws IllegalStateException")
        void doubleCancel() {
            OrderId orderId = seedOrder();

            cancelOrderService.cancel(new CancelOrderCommand(orderId, "key-1", "hash-1"));
            assertThatThrownBy(() -> cancelOrderService.cancel(
                    new CancelOrderCommand(orderId, "key-2", "hash-2")))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}