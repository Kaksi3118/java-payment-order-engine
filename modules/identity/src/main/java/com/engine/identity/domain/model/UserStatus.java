package com.engine.identity.domain.model;

/**
 * Lifecycle state of a {@link User}.
 *
 * <p>State machine:
 * <pre>
 *                  register()
 *       (new) ────────────────────► PENDING
 *                                   │
 *                                   │ activate()
 *                                   ▼
 *                                  ACTIVE ◄────────────┐
 *                                   │  ▲               │
 *                       suspend()    │  │ activate()   │
 *                                   ▼  │               │
 *                                SUSPENDED ────────────┘
 *
 *                   deactivate()
 *       ACTIVE ──────────────────► DEACTIVATED   (terminal)
 * </pre>
 *
 * <p>{@link com.engine.identity.domain.port.in.AuthenticateUserUseCase} requires the
 * {@link User} to be {@link #ACTIVE} &mdash; this is the single domain-side condition that
 * distinguishes &quot;registered but unverified&quot; from &quot;can login&quot;.
 */
public enum UserStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    DEACTIVATED
}