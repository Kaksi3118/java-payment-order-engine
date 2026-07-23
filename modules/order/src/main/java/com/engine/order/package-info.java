/**
 * Order Bounded Context — order lifecycle, inventory reservation, and the
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
 *
 * <p><strong>Dependency rule:</strong> {@code domain} depends on nothing but the
 * shared kernel. {@code application} depends on {@code domain}. {@code adapters}
 * depend on {@code application} (driving) or implement {@code application} ports
 * (driven). The {@code domain} package never depends on {@code adapters}.
 */
package com.engine.order;