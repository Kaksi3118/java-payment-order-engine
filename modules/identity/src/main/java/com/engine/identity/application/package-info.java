/**
 * Identity application layer &mdash; use-case orchestration.
 *
 * <p>Each class here implements a driving port from {@code domain.port.in} and coordinates one
 * business transaction: checking preconditions, delegating to driven ports (repository, hasher,
 * JWT issuer, outbox), and returning a minimal result DTO. Spring's {@code @Service} and
 * {@code @Transactional} live here &mdash; never in the domain layer.
 */
package com.engine.identity.application;