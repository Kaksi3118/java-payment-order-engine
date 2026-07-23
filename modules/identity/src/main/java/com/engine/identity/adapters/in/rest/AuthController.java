package com.engine.identity.adapters.in.rest;

import com.engine.identity.adapters.in.rest.dto.LoginRequest;
import com.engine.identity.adapters.in.rest.dto.LoginResponse;
import com.engine.identity.adapters.in.rest.dto.RegisterRequest;
import com.engine.identity.adapters.in.rest.dto.RegisterResponse;
import com.engine.identity.domain.model.Email;
import com.engine.identity.domain.model.JwtTokens;
import com.engine.identity.domain.model.Role;
import com.engine.identity.domain.model.Roles;
import com.engine.identity.domain.port.in.AuthenticateUserUseCase;
import com.engine.identity.domain.port.in.AuthenticateUserUseCase.AuthenticateUserCommand;
import com.engine.identity.domain.port.in.RegisterUserUseCase;
import com.engine.identity.domain.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.engine.identity.domain.port.in.RegisterUserUseCase.RegisterUserResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * REST adapter: authentication endpoints.
 *
 * <p>Two endpoints, both under {@code /api/auth/**} (permitted by {@code SecurityConfig}):
 * <ul>
 *     <li>{@code POST /api/auth/register} &mdash; mutating endpoint; requires an
 *         {@code Idempotency-Key} header (AGENTS.md invariant #4). Returns 201 + the new user's ID.</li>
 *     <li>{@code POST /api/auth/login} &mdash; read-only endpoint; no idempotency key needed
 *         (repeated logins just re-issue tokens). Returns 200 + JWT access/refresh tokens.</li>
 * </ul>
 *
 * <p>The controller is intentionally thin: it maps HTTP to use-case commands, delegates to the
 * application layer, and maps use-case results back to HTTP DTOs. Domain exceptions propagate
 * to the {@link GlobalExceptionHandler} for HTTP status mapping.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;

    public AuthController(RegisterUserUseCase registerUserUseCase,
                          AuthenticateUserUseCase authenticateUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RegisterRequest request) {

        RegisterUserCommand command = new RegisterUserCommand(
                Email.of(request.email()),
                request.password(),
                toRoles(request.roles()));

        RegisterUserResult result = registerUserUseCase.register(command);

        return ResponseEntity.ok(new RegisterResponse(result.userId()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        AuthenticateUserCommand command = new AuthenticateUserCommand(
                Email.of(request.email()),
                request.password());

        JwtTokens tokens = authenticateUserUseCase.authenticate(command);

        return ResponseEntity.ok(LoginResponse.of(tokens.accessToken(), tokens.refreshToken()));
    }

    private static Roles toRoles(java.util.List<String> roleNames) {
        Set<Role> roleSet = new LinkedHashSet<>();
        for (String name : roleNames) {
            roleSet.add(Role.valueOf(name.toUpperCase()));
        }
        return Roles.from(roleSet);
    }
}