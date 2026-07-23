/**
 * Security adapters &mdash; BCrypt password hashing ({@link com.engine.identity.adapters.out.security.BcryptPasswordHasher})
 * and JWT issuance ({@link com.engine.identity.adapters.out.security.JwtIssuerAdapter}), plus the
 * Spring Security filter chain that protects every non-auth endpoint of the Identity context via
 * an OAuth2 JWT resource server.
 */
package com.engine.identity.adapters.out.security;