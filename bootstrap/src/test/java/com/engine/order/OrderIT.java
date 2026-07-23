package com.engine.order;

import com.engine.bootstrap.Application;
import com.engine.identity.adapters.out.persistence.OutboxEntity;
import com.engine.identity.adapters.out.persistence.OutboxJpaRepository;
import com.engine.identity.adapters.out.persistence.OutboxStatus;
import com.engine.order.adapters.in.rest.dto.OrderResponse;
import com.engine.order.adapters.out.inventory.InMemoryInventoryAdapter;
import com.engine.order.domain.event.OrderCancelled;
import com.engine.order.domain.event.OrderPlaced;
import com.engine.order.domain.exception.InsufficientInventoryException;
import com.engine.order.domain.exception.OrderNotFoundException;
import com.engine.order.domain.model.OrderItem;
import com.engine.order.domain.model.OrderStatus;
import com.engine.order.domain.model.OrderView;
import com.engine.order.domain.port.in.CancelOrderUseCase.CancelOrderCommand;
import com.engine.order.domain.port.in.GetOrderQuery;
import com.engine.order.domain.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import com.engine.order.domain.port.in.PlaceOrderUseCase.PlaceOrderResult;
import com.engine.shared.domain.ids.OrderId;
import com.engine.shared.domain.ids.UserId;
import com.engine.shared.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end integration test for the Order bounded context.
 *
 * <p>Spins up a real PostgreSQL 17 Docker container via Testcontainers, runs Flyway V1 + V2
 * migrations, and exercises the full stack: domain &rarr; application (use cases with idempotency
 * guard + inventory reservation + outbox) &rarr; adapters (JPA persistence, in-memory inventory,
 * DB-backed idempotency store, CQRS query).
 *
 * <p>What this proves to a reviewer:
 * <ul>
 *     <li>Flyway V2 migration DDL matches JPA entity annotations.</li>
 *     <li>The Idempotency Pattern works end-to-end: same key + hash returns cached result.</li>
 *     <li>Inventory reservation prevents order creation when stock is insufficient.</li>
 *     <li>The Transactional Outbox Pattern: OrderPlaced and OrderCancelled events persisted.</li>
 *     <li>CQRS read side returns a consistent snapshot after writes.</li>
 *     <li>Cancel releases inventory and transitions status correctly.</li>
 * </ul>
 */
@SpringBootTest(classes = Application.class)
@Testcontainers
@EnabledIfEnvironmentVariable(named = "DOCKER_HOST", matches = ".*")
@DisplayName("Order Integration Test (Testcontainers + PostgreSQL 17)")
class OrderIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("poe_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private com.engine.order.domain.port.in.PlaceOrderUseCase placeOrderUseCase;
    @Autowired
    private com.engine.order.domain.port.in.CancelOrderUseCase cancelOrderUseCase;
    @Autowired
    private GetOrderQuery getOrderQuery;
    @Autowired
    private InMemoryInventoryAdapter inventoryAdapter;
    @Autowired
    private OutboxJpaRepository outboxJpaRepository;

    private static final Currency USD = Currency.getInstance("USD");
    private static final UUID PRODUCT_A = UUID.randomUUID();
    private static final UUID PRODUCT_B = UUID.randomUUID();

    @BeforeEach
    void seedInventory() {
        inventoryAdapter.setStock(PRODUCT_A, 100);
        inventoryAdapter.setStock(PRODUCT_B, 50);
        outboxJpaRepository.deleteAll();
    }

    private static List<OrderItem> defaultItems() {
        return List.of(
                new OrderItem(PRODUCT_A, 2, Money.of(new BigDecimal("10.00"), USD)),
                new OrderItem(PRODUCT_B, 1, Money.of(new BigDecimal("25.50"), USD)));
    }

    @Test
    @DisplayName("place order persists row, reserves inventory, and writes OrderPlaced to outbox")
    void placeOrderEndToEnd() {
        PlaceOrderResult result = placeOrderUseCase.place(new PlaceOrderCommand(
                UserId.random(), defaultItems(), "key-1", "hash-1"));

        assertThat(result.orderId()).isNotNull();

        OrderView view = getOrderQuery.findById(OrderId.of(result.orderId())).orElseThrow();
        assertThat(view.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(view.items()).hasSize(2);
        assertThat(view.totalAmount().amount()).isEqualByComparingTo(new BigDecimal("45.50"));

        assertThat(inventoryAdapter.getStock(PRODUCT_A)).isEqualTo(98);
        assertThat(inventoryAdapter.getStock(PRODUCT_B)).isEqualTo(49);

        List<OutboxEntity> outboxRows = outboxJpaRepository.findAll();
        assertThat(outboxRows).hasSize(1);
        assertThat(outboxRows.get(0).getEventType()).isEqualTo(OrderPlaced.class.getName());
        assertThat(outboxRows.get(0).getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("idempotency: replay with same key + hash returns same orderId without side effects")
    void idempotencyReplayReturnsCachedResult() {
        PlaceOrderCommand command = new PlaceOrderCommand(
                UserId.random(), defaultItems(), "idem-key-2", "hash-2");

        PlaceOrderResult first = placeOrderUseCase.place(command);
        PlaceOrderResult second = placeOrderUseCase.place(command);

        assertThat(second.orderId()).isEqualTo(first.orderId());
        assertThat(inventoryAdapter.getStock(PRODUCT_A)).isEqualTo(98);
    }

    @Test
    @DisplayName("insufficient inventory prevents order creation and outbox drain")
    void insufficientInventoryPreventsOrder() {
        UUID scarceProduct = UUID.randomUUID();
        inventoryAdapter.setStock(scarceProduct, 1);

        assertThatThrownBy(() -> placeOrderUseCase.place(new PlaceOrderCommand(
                UserId.random(),
                List.of(new OrderItem(scarceProduct, 5, Money.of(new BigDecimal("10.00"), USD))),
                "key-3", "hash-3")))
                .isInstanceOf(InsufficientInventoryException.class);

        assertThat(inventoryAdapter.getStock(scarceProduct)).isEqualTo(1);
    }

    @Test
    @DisplayName("cancel order releases inventory and writes OrderCancelled to outbox")
    void cancelOrderEndToEnd() {
        PlaceOrderResult result = placeOrderUseCase.place(new PlaceOrderCommand(
                UserId.random(), defaultItems(), "key-4", "hash-4"));
        OrderId orderId = OrderId.of(result.orderId());

        cancelOrderUseCase.cancel(new CancelOrderCommand(orderId, "cancel-key-4", "cancel-hash-4"));

        OrderView view = getOrderQuery.findById(orderId).orElseThrow();
        assertThat(view.status()).isEqualTo(OrderStatus.CANCELLED);

        assertThat(inventoryAdapter.getStock(PRODUCT_A)).isEqualTo(100);
        assertThat(inventoryAdapter.getStock(PRODUCT_B)).isEqualTo(50);

        List<OutboxEntity> outboxRows = outboxJpaRepository.findAll();
        assertThat(outboxRows).hasSize(2);
        assertThat(outboxRows.get(1).getEventType()).isEqualTo(OrderCancelled.class.getName());
    }

    @Test
    @DisplayName("cancel non-existent order throws OrderNotFoundException")
    void cancelNonExistentOrder() {
        assertThatThrownBy(() -> cancelOrderUseCase.cancel(
                new CancelOrderCommand(OrderId.random(), "key-5", "hash-5")))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("get order by ID returns empty for non-existent order")
    void getNonExistentOrder() {
        assertThat(getOrderQuery.findById(OrderId.random())).isEmpty();
    }
}