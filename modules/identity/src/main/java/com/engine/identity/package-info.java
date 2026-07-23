/**
 * Identity Bounded Context — authentication, JWT issuance/verification, and RBAC.
 *
 * <p>Hexagonal layering (enforced by ArchUnit tests, see
 * {@code docs/architecture/}):
 * <pre>
 * com.engine.identity
 *     .domain           // entities, value objects, domain services, ports
 *     .application      // use cases (commands + queries), orchestration
 *     .adapters.in      // REST controllers, inbound message consumers
 *     .adapters.out     // JPA persistence, outbound messaging, external clients
 * </pre>
 *
 * <p><strong>Dependency rule:</strong> {@code domain} depends on nothing but the
 * shared kernel. {@code application} depends on {@code domain}. {@code adapters}
 * depend on {@code application} (driving) or implement {@code application} ports
 * (driven). The {@code domain} package never depends on {@code adapters}.
 */
package com.engine.identity;