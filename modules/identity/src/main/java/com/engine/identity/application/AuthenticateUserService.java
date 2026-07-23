package com.engine.identity.application;

import com.engine.identity.domain.exception.InvalidCredentialsException;
import com.engine.identity.domain.exception.UserNotActiveException;
import com.engine.identity.domain.model.JwtTokens;
import com.engine.identity.domain.model.User;
import com.engine.identity.domain.port.in.AuthenticateUserUseCase;
import com.engine.identity.domain.port.out.JwtIssuer;
import com.engine.identity.domain.port.out.PasswordHasher;
import com.engine.identity.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Use case: authenticate an existing user and issue JWT tokens.
 *
 * <p>Read-only operation ({@code @Transactional(readOnly = true)}) that:
 * <ol>
 *     <li>Looks up the user by email. A missing user and a wrong password both surface as
 *         {@link InvalidCredentialsException} &mdash; identical messages prevent user
 *         enumeration.</li>
 *     <li>Verifies the raw password against the stored hash via {@link PasswordHasher#matches}.
 *         Wrong password &rarr; {@link InvalidCredentialsException}.</li>
 *     <li>Checks the lifecycle guard: only {@link com.engine.identity.domain.model.UserStatus#ACTIVE}
 *         users may authenticate. Non-active &rarr; {@link UserNotActiveException}.</li>
 *     <li>Delegates to {@link JwtIssuer#issue} to produce access + refresh tokens.</li>
 * </ol>
 */
@Service
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtIssuer jwtIssuer;

    public AuthenticateUserService(UserRepository userRepository,
                                   PasswordHasher passwordHasher,
                                   JwtIssuer jwtIssuer) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository must not be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "PasswordHasher must not be null");
        this.jwtIssuer = Objects.requireNonNull(jwtIssuer, "JwtIssuer must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public JwtTokens authenticate(AuthenticateUserCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(command.rawPassword(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            throw new UserNotActiveException(user.status());
        }

        return jwtIssuer.issue(user);
    }
}