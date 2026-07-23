package com.engine.order.adapters.in.rest;

import com.engine.order.adapters.in.rest.dto.PlaceOrderResponse;
import com.engine.order.domain.exception.IdempotencyConflictException;
import com.engine.order.domain.exception.InsufficientInventoryException;
import com.engine.order.domain.exception.OrderNotFoundException;
import com.engine.order.domain.model.OrderStatus;
import com.engine.order.domain.model.OrderView;
import com.engine.order.domain.port.in.CancelOrderUseCase;
import com.engine.order.domain.port.in.GetOrderQuery;
import com.engine.order.domain.port.in.PlaceOrderUseCase;
import com.engine.order.domain.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import com.engine.order.domain.port.in.PlaceOrderUseCase.PlaceOrderResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {OrderController.class, OrderExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {OrderController.class, OrderExceptionHandler.class, OrderControllerTest.TestClockConfig.class})
@DisplayName("OrderController")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlaceOrderUseCase placeOrderUseCase;

    @MockBean
    private CancelOrderUseCase cancelOrderUseCase;

    @MockBean
    private GetOrderQuery getOrderQuery;

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    private static final String VALID_PLACE_BODY = """
            {"customerId":"550e8400-e29b-41d4-a716-446655440000",
             "items":[{"productId":"660e8400-e29b-41d4-a716-446655440001","quantity":2,"unitPrice":10.00,"currency":"USD"}]}""";

    @Nested
    @DisplayName("POST /api/orders")
    class PlaceOrder {

        @Test
        @DisplayName("returns 201 + orderId when idempotency key is present and body is valid")
        void returns201WithOrderId() throws Exception {
            UUID orderId = UUID.randomUUID();
            when(placeOrderUseCase.place(any(PlaceOrderCommand.class)))
                    .thenReturn(new PlaceOrderResult(orderId));

            mockMvc.perform(post("/api/orders")
                            .header("Idempotency-Key", "abc-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_PLACE_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderId").value(orderId.toString()));
        }

        @Test
        @DisplayName("returns 400 when Idempotency-Key header is missing")
        void returns400WhenIdempotencyKeyMissing() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_PLACE_BODY))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 409 when insufficient inventory")
        void returns409OnInsufficientInventory() throws Exception {
            when(placeOrderUseCase.place(any(PlaceOrderCommand.class)))
                    .thenThrow(new InsufficientInventoryException(List.of(UUID.randomUUID())));

            mockMvc.perform(post("/api/orders")
                            .header("Idempotency-Key", "abc-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_PLACE_BODY))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 on idempotency conflict (same key, different body)")
        void returns409OnIdempotencyConflict() throws Exception {
            when(placeOrderUseCase.place(any(PlaceOrderCommand.class)))
                    .thenThrow(new IdempotencyConflictException("abc-123"));

            mockMvc.perform(post("/api/orders")
                            .header("Idempotency-Key", "abc-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_PLACE_BODY))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /api/orders/{id}/cancel")
    class CancelOrder {

        @Test
        @DisplayName("returns 204 when cancel succeeds")
        void returns204OnSuccess() throws Exception {
            UUID orderId = UUID.randomUUID();

            mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                            .header("Idempotency-Key", "cancel-key-1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("returns 404 when order not found")
        void returns404OnNotFound() throws Exception {
            UUID orderId = UUID.randomUUID();
            doThrow(new OrderNotFoundException(orderId))
                    .when(cancelOrderUseCase).cancel(any());

            mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                            .header("Idempotency-Key", "cancel-key-1"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 409 when order is in a non-cancellable state")
        void returns409OnIllegalState() throws Exception {
            UUID orderId = UUID.randomUUID();
            doThrow(new java.lang.IllegalStateException("Cannot cancel"))
                    .when(cancelOrderUseCase).cancel(any());

            mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                            .header("Idempotency-Key", "cancel-key-1"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 400 when Idempotency-Key is missing")
        void returns400WhenIdempotencyKeyMissing() throws Exception {
            mockMvc.perform(post("/api/orders/{id}/cancel", UUID.randomUUID()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/orders/{id}")
    class GetOrder {

        @Test
        @DisplayName("returns 200 + order snapshot when order exists")
        void returns200WithOrder() throws Exception {
            UUID orderId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            OrderView view = new OrderView(
                    orderId, customerId, OrderStatus.CREATED, Currency.getInstance("USD"),
                    List.of(new com.engine.order.domain.model.OrderItem(
                            productId, 2, com.engine.shared.domain.model.Money.of(new BigDecimal("10.00"), Currency.getInstance("USD")))),
                    com.engine.shared.domain.model.Money.of(new BigDecimal("20.00"), Currency.getInstance("USD")),
                    Instant.parse("2026-07-23T12:00:00Z"), Instant.parse("2026-07-23T12:00:00Z"));

            when(getOrderQuery.findById(any())).thenReturn(Optional.of(view));

            mockMvc.perform(get("/api/orders/{id}", orderId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                    .andExpect(jsonPath("$.status").value("CREATED"))
                    .andExpect(jsonPath("$.items[0].productId").value(productId.toString()));
        }

        @Test
        @DisplayName("returns 404 when order not found")
        void returns404WhenNotFound() throws Exception {
            when(getOrderQuery.findById(any())).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/orders/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }
}