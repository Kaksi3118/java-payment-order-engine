/**
 * Identity adapters &mdash; implementations of the domain's driving (in) and driven (out) ports.
 *
 * <p>Hexagonal convention: this package depends on the application layer (its use cases) and on
 * the domain layer (the ports it implements). The reverse dependency &mdash; domain or application
 * depending on adapters &mdash; is rejected by ArchUnit (see
 * {@link com.engine.architecture.ArchitectureArchTest}).
 *
 * @see com.engine.identity.adapters.in
 * @see com.engine.identity.adapters.out
 */
package com.engine.identity.adapters;