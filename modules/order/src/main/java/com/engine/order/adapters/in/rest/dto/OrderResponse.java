package com.engine.order.adapters.in.rest.dto;

import com.engine.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response body for {@code GET /api/orders/{id}} &mdash; a read-only snapshot of the order.
 */
public record OrderResponse(
        UUID orderId,
        UUID customerId,
        OrderStatus status,
        String currency,
        List<LineItem> items,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt
) {

    public record LineItem(UUID productId, int quantity, BigDecimal unitPrice, String currency) {
    }
}