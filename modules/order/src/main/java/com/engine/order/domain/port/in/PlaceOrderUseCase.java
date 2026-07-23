package com.engine.order.domain.port.in;

import com.engine.order.domain.model.OrderItem;
import com.engine.shared.domain.ids.UserId;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Driving port (use-case contract): place a new order.
 */
public interface PlaceOrderUseCase {

    PlaceOrderResult place(PlaceOrderCommand command);

    record PlaceOrderCommand(
            UserId customerId,
            List<OrderItem> items,
            String idempotencyKey,
            String requestHash
    ) {

        public PlaceOrderCommand {
            Objects.requireNonNull(customerId, "customerId must not be null");
            Objects.requireNonNull(items, "items must not be null");
            Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
            Objects.requireNonNull(requestHash, "requestHash must not be null");
            if (items.isEmpty()) {
                throw new IllegalArgumentException("items must not be empty");
            }
            if (idempotencyKey.isBlank()) {
                throw new IllegalArgumentException("idempotencyKey must not be blank");
            }
        }
    }

    record PlaceOrderResult(UUID orderId) {

        public PlaceOrderResult {
            Objects.requireNonNull(orderId, "orderId must not be null");
        }
    }
}