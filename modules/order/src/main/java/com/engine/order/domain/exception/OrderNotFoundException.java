package com.engine.order.domain.exception;

/**
 * Thrown when an order is not found by ID. The application layer maps this to HTTP 404.
 */
public final class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(java.util.UUID orderId) {
        super("Order not found: " + orderId);
    }
}