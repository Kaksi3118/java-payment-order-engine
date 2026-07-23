/**
 * Payment Bounded Context — payment authorization, capture, and refund; integration
 * with an external payment gateway protected by Resilience4j (circuit breaker,
 * retry, rate limiter).
 *
 * <p>Hexagonal layering (enforced by ArchUnit tests, see
 * {@code docs/architecture/}):
 * <pre>
 * com.engine.payment
 *     .domain           // Transaction, Refund, PaymentStatus, domain services, ports
 *     .application      // AuthorizePaymentCommand, payment orchestration
 *     .adapters.in      // @RestController, @RabbitListener inbound consumers
 *     .adapters.out     // JPA repositories, gateway client (WebClient + Resilience4j)
 * </pre>
 *
 * <p><strong>Dependency rule:</strong> {@code domain} depends on nothing but the
 * shared kernel. {@code application} depends on {@code domain}. {@code adapters}
 * depend on {@code application} (driving) or implement {@code application} ports
 * (driven). The {@code domain} package never depends on {@code adapters}.
 */
package com.engine.payment;