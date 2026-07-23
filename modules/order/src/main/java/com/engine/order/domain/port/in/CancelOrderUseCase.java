package com.engine.order.domain.port.in;

import com.engine.shared.domain.ids.OrderId;

import java.util.Objects;

/**
 * Driving port (use-case contract): cancel an existing order.
 */
public interface CancelOrderUseCase {

    void cancel(CancelOrderCommand command);

    record CancelOrderCommand(OrderId orderId, String idempotencyKey, String requestHash) {

        public CancelOrderCommand {
            Objects.requireNonNull(orderId, "orderId must not be null");
            Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
            Objects.requireNonNull(requestHash, "requestHash must not be null");
        }
    }
}