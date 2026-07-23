package com.engine.order.application;

import com.engine.order.domain.exception.InsufficientInventoryException;
import com.engine.order.domain.event.OrderPlaced;
import com.engine.order.domain.model.OrderItem;
import com.engine.order.domain.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import com.engine.order.domain.port.in.PlaceOrderUseCase.PlaceOrderResult;
import com.engine.shared.domain.ids.UserId;
import com.engine.shared.domain.model.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
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

@DisplayName("PlaceOrderService")
class PlaceOrderServiceTest {

    private FakeOrderRepository orderRepository;
    private FakeInventoryPort inventoryPort;
    private FakeIdempotencyPort idempotencyPort;
    private FakeEventOutbox eventOutbox;
    private PlaceOrderService service;
    private MeterRegistry meterRegistry;

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC);
    private static final Currency USD = Currency.getInstance("USD");

    private static PlaceOrderCommand newCommand(UUID productId) {
        return new PlaceOrderCommand(
                UserId.random(),
                List.of(new OrderItem(productId, 2, Money.of(new BigDecimal("10.00"), USD))),
                "idem-key-1",
                "hash-1");
    }

    @BeforeEach
    void setUp() {
        orderRepository = new FakeOrderRepository();
        inventoryPort = new FakeInventoryPort();
        idempotencyPort = new FakeIdempotencyPort();
        eventOutbox = new FakeEventOutbox();
        meterRegistry = new SimpleMeterRegistry();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        service = new PlaceOrderService(orderRepository, inventoryPort, idempotencyPort,
                eventOutbox, objectMapper, FIXED_CLOCK, meterRegistry);
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("places order, reserves inventory, drains event to outbox, saves idempotency result")
        void placesOrderEndToEnd() {
            UUID productId = UUID.randomUUID();
            PlaceOrderCommand command = newCommand(productId);

            PlaceOrderResult result = service.place(command);

            assertThat(result.orderId()).isNotNull();
            assertThat(orderRepository.size()).isEqualTo(1);
            assertThat(inventoryPort.isReserved(result.orderId())).isTrue();
            assertThat(eventOutbox.size()).isEqualTo(1);
            assertThat(eventOutbox.events().get(0)).isInstanceOf(OrderPlaced.class);
            assertThat(idempotencyPort.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("idempotency")
    class IdempotencyGuard {

        @Test
        @DisplayName("second call with same key + hash returns cached result without re-executing")
        void replayReturnsCachedResult() {
            UUID productId = UUID.randomUUID();
            PlaceOrderCommand command = newCommand(productId);

            PlaceOrderResult first = service.place(command);
            PlaceOrderResult second = service.place(command);

            assertThat(second.orderId()).isEqualTo(first.orderId());
            assertThat(orderRepository.size()).isEqualTo(1);
            assertThat(eventOutbox.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("same key with different hash throws IdempotencyConflictException")
        void sameKeyDifferentHashThrows() {
            UUID productId = UUID.randomUUID();
            PlaceOrderCommand first = newCommand(productId);

            service.place(first);

            PlaceOrderCommand second = new PlaceOrderCommand(
                    UserId.random(),
                    List.of(new OrderItem(productId, 5, Money.of(new BigDecimal("99.00"), USD))),
                    "idem-key-1",
                    "different-hash");

            assertThatThrownBy(() -> service.place(second))
                    .isInstanceOf(com.engine.order.domain.exception.IdempotencyConflictException.class);
        }
    }

    @Nested
    @DisplayName("inventory failure")
    class InventoryFailure {

        @Test
        @DisplayName("insufficient inventory prevents order creation and outbox drain")
        void insufficientInventoryPreventsOrder() {
            UUID productId = UUID.randomUUID();
            inventoryPort.markUnavailable(productId);

            PlaceOrderCommand command = newCommand(productId);

            assertThatThrownBy(() -> service.place(command))
                    .isInstanceOf(InsufficientInventoryException.class);

            assertThat(orderRepository.size()).isZero();
            assertThat(eventOutbox.size()).isZero();
        }
    }

    @Nested
    @DisplayName("null checks")
    class NullChecks {

        @Test
        @DisplayName("constructor rejects null dependencies")
        void constructorRejectsNulls() {
            ObjectMapper objectMapper = new ObjectMapper();
            assertThatThrownBy(() -> new PlaceOrderService(null, inventoryPort, idempotencyPort, eventOutbox, objectMapper, FIXED_CLOCK, meterRegistry))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new PlaceOrderService(orderRepository, null, idempotencyPort, eventOutbox, objectMapper, FIXED_CLOCK, meterRegistry))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new PlaceOrderService(orderRepository, inventoryPort, null, eventOutbox, objectMapper, FIXED_CLOCK, meterRegistry))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new PlaceOrderService(orderRepository, inventoryPort, idempotencyPort, null, objectMapper, FIXED_CLOCK, meterRegistry))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new PlaceOrderService(orderRepository, inventoryPort, idempotencyPort, eventOutbox, null, FIXED_CLOCK, meterRegistry))
                    .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new PlaceOrderService(orderRepository, inventoryPort, idempotencyPort, eventOutbox, objectMapper, null, meterRegistry))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}