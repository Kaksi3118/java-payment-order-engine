/**
 * Cross-cutting driven ports shared by all bounded contexts.
 *
 * <p>Currently exposes {@link com.engine.shared.domain.port.out.EventOutbox} &mdash; the single
 * integration point between an application service and the transactional outbox table. Every
 * bounded context's application layer depends on this port; each context's adapter module
 * provides the concrete JPA implementation.
 */
package com.engine.shared.domain.port.out;