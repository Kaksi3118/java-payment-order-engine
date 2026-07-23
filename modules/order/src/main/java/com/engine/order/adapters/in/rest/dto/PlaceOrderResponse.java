package com.engine.order.adapters.in.rest.dto;

import java.util.UUID;

/**
 * Response body for {@code POST /api/orders}.
 */
public record PlaceOrderResponse(UUID orderId) {
}