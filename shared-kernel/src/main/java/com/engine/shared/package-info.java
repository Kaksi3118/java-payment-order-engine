/**
 * Shared Kernel — common domain primitives shared across bounded contexts.
 *
 * <p>Pure Java, framework-agnostic. Contains:
 * <ul>
 *   <li>Typed identifiers (e.g. {@code OrderId}, {@code PaymentId}) — avoiding primitive obsession.</li>
 *   <li>{@code Money} value object with currency-aware arithmetic.</li>
 *   <li>Base classes for domain events and audit metadata.</li>
 * </ul>
 *
 * <p><strong>Dependency rule:</strong> this module MUST NOT depend on Spring, JPA, or
 * any delivery mechanism. It is referenced by every bounded context.
 */
package com.engine.shared;