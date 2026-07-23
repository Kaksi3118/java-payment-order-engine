package com.engine.order.application;

import com.engine.order.domain.model.OrderItem;
import com.engine.order.domain.model.OrderView;
import com.engine.order.domain.port.in.GetOrderQuery;
import com.engine.order.domain.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import com.engine.order.domain.port.in.PlaceOrderUseCase.PlaceOrderResult;
import com.engine.shared.domain.ids.OrderId;
import com.engine.shared.domain.ids.UserId;
import com.engine.shared.domain.model.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetOrderQueryHandler")
class GetOrderQueryHandlerTest {

    private FakeOrderRepository orderRepository;
    private FakeInventoryPort inventoryPort;
    private FakeIdempotencyPort idempotencyPort;
    private FakeEventOutbox eventOutbox;
    private GetOrderQueryHandler queryHandler;
    private PlaceOrderService placeOrderService;

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
        queryHandler = new GetOrderQueryHandler(orderRepository);
    }

    @Test
    @DisplayName("findById returns OrderView snapshot for an existing order")
    void findByIdReturnsView() {
        UUID productId = UUID.randomUUID();
        PlaceOrderResult result = placeOrderService.place(new PlaceOrderCommand(
                UserId.random(),
                List.of(new OrderItem(productId, 3, Money.of(new BigDecimal("15.00"), USD))),
                "key-1",
                "hash-1"));

        GetOrderQuery query = queryHandler;
        var view = query.findById(OrderId.of(result.orderId()));

        assertThat(view).isPresent();
        assertThat(view.get().orderId()).isEqualTo(result.orderId());
        assertThat(view.get().items()).hasSize(1);
        assertThat(view.get().items().get(0).productId()).isEqualTo(productId);
        assertThat(view.get().totalAmount().amount()).isEqualByComparingTo(new BigDecimal("45.00"));
    }

    @Test
    @DisplayName("findById returns empty for a non-existent order")
    void findByIdEmpty() {
        assertThat(queryHandler.findById(OrderId.random())).isEmpty();
    }
}