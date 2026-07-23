/**
 * Order Bounded Context &mdash; order lifecycle, inventory reservation, and the
 * transactional outbox pattern for reliable event publication.
 *
 * <p>Hexagonal layering (enforced by ArchUnit tests, see
 * {@code docs/architecture/}):
 * <pre>
 * com.engine.order
 *     .domain           // Order, OrderItem, OrderStatus, domain services, ports
 *     .application      // PlaceOrderCommand, OrderQuery, outbox/idempotency orchestration
 *     .adapters.in      // @RestController, @RabbitListener inbound consumers
 *     .adapters.out     // JPA repositories, outbox publisher, external inventory client
 * </pre>
 */
package com.engine.order;