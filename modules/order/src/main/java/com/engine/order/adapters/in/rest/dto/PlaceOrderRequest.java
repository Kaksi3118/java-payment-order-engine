package com.engine.order.adapters.in.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request body for {@code POST /api/orders}.
 */
public record PlaceOrderRequest(
        @NotNull(message = "customerId must not be null")
        String customerId,

        @NotNull(message = "items must not be null")
        @Size(min = 1, message = "items must contain at least one element")
        List<@NotNull LineItem> items
) {

    public record LineItem(
            @NotBlank(message = "productId must not be blank")
            String productId,

            @Min(value = 1, message = "quantity must be at least 1")
            int quantity,

            @NotNull(message = "unitPrice must not be null")
            BigDecimal unitPrice,

            @NotBlank(message = "currency must not be blank")
            String currency
    ) {
    }
}