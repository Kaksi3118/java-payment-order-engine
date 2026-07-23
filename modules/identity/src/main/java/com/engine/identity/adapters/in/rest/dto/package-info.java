/**
 * Request and response DTOs for the Identity REST API.
 *
 * <p>All DTOs are Java 21 records &mdash; immutable, concise, and automatically serialized by
 * Jackson. They never expose the domain {@link com.engine.identity.domain.model.User} aggregate
 * directly; the controller maps between DTOs and use-case commands/results.
 */
package com.engine.identity.adapters.in.rest.dto;