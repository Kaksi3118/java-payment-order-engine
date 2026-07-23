package com.engine.identity.domain.model;

/**
 * Coarse-grained roles for Role-Based Access Control (RBAC).
 *
 * <p>Kept deliberately small for the showcase. Concrete permissions (e.g. &quot;payment:refund&quot;)
 * can be derived from a role-permission mapping in the application or authorization layer; the
 * domain's job is only to know which roles a {@link User} holds.
 */
public enum Role {
    CUSTOMER,
    ADMIN,
    SUPPORT
}