/**
 * Bootstrap — Spring Boot deployment module.
 *
 * <p>Wires adapters from {@code identity}, {@code order}, and {@code payment}
 * bounded contexts into a single runnable jar (modular monolith). Future
 * extraction to microservices is enabled by the strict module boundaries enforced
 * upstream via ArchUnit.
 */
package com.engine.bootstrap;