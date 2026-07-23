package com.engine.order.domain.model;

/**
 * Lifecycle state of an {@link Order}.
 *
 * <p>State machine:
 * <pre>
 *                  place()
 *       (new) ───────────────────► CREATED
 *                                   │
 *                    confirm()      │  cancel()
 *                                   ▼  ──────────────► CANCELLED
 *                                  CONFIRMED
 *                                   │  │
 *                    ship()         │  │  cancel()
 *                                   ▼  │  ──────────────► CANCELLED
 *                                  SHIPPED
 *                                   │
 *                    deliver()      │
 *                                   ▼
 *                                 DELIVERED   (terminal)
 * </pre>
 *
 * <p>Cancellation is allowed from CREATED and CONFIRMED, but NOT from SHIPPED
 * (goods are in transit &mdash; cancellation must go through a return flow)
 * or DELIVERED (terminal &mdash; use the refund flow in the Payment context).
 */
public enum OrderStatus {
    CREATED,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}