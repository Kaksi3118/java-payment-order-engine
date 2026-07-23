package com.engine.identity.adapters.in.rest;

import com.engine.identity.adapters.in.rest.dto.LoginResponse;
import com.engine.identity.adapters.in.rest.dto.RegisterResponse;
import com.engine.identity.domain.exception.EmailAlreadyRegisteredException;
import com.engine.identity.domain.exception.InvalidCredentialsException;
import com.engine.identity.domain.exception.UserNotActiveException;
import com.engine.identity.domain.model.Email;
import com.engine.identity.domain.model.JwtTokens;
import com.engine.identity.domain.model.Roles;
import com.engine.identity.domain.model.UserStatus;
import com.engine.identity.domain.port.in.AuthenticateUserUseCase;
import com.engine.identity.domain.port.in.AuthenticateUserUseCase.AuthenticateUserCommand;
import com.engine.identity.domain.port.in.RegisterUserUseCase;
import com.engine.identity.domain.port.in.RegisterUserUseCase.RegisterUserCommand;
import com.engine.identity.domain.port.in.RegisterUserUseCase.RegisterUserResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {AuthController.class, GlobalExceptionHandler.class, AuthControllerTest.TestClockConfig.class})
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;

    @MockBean
    private AuthenticateUserUseCase authenticateUserUseCase;

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-07-23T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    private static final String VALID_REGISTER_BODY = """
            {"email":"alice@example.com","password":"p@ssw0rd","roles":["CUSTOMER"]}""";
    private static final String VALID_LOGIN_BODY = """
            {"email":"alice@example.com","password":"p@ssw0rd"}""";

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("returns 200 + userId when idempotency key is present and body is valid")
        void returns200WithUserId() throws Exception {
            UUID userId = UUID.randomUUID();
            when(registerUserUseCase.register(any(RegisterUserCommand.class)))
                    .thenReturn(new RegisterUserResult(userId));

            mockMvc.perform(post("/api/auth/register")
                            .header("Idempotency-Key", "abc-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REGISTER_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(userId.toString()));
        }

        @Test
        @DisplayName("returns 400 when Idempotency-Key header is missing")
        void returns400WhenIdempotencyKeyMissing() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REGISTER_BODY))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Idempotency-Key")));
        }

        @Test
        @DisplayName("returns 400 when email is blank")
        void returns400WhenEmailBlank() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .header("Idempotency-Key", "abc-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"","password":"p@ssw0rd","roles":["CUSTOMER"]}"""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when password is shorter than 8 characters")
        void returns400WhenPasswordTooShort() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .header("Idempotency-Key", "abc-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"alice@example.com","password":"short","roles":["CUSTOMER"]}"""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 409 when email is already registered")
        void returns409OnDuplicateEmail() throws Exception {
            when(registerUserUseCase.register(any(RegisterUserCommand.class)))
                    .thenThrow(new EmailAlreadyRegisteredException(Email.of("alice@example.com")));

            mockMvc.perform(post("/api/auth/register")
                            .header("Idempotency-Key", "abc-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_REGISTER_BODY))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("alice@example.com")));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("returns 200 + access and refresh tokens on valid credentials")
        void returns200WithTokens() throws Exception {
            JwtTokens tokens = new JwtTokens(
                    "access-token-value",
                    "refresh-token-value",
                    Instant.parse("2026-07-23T12:30:00Z"),
                    Instant.parse("2026-07-24T12:00:00Z"));
            when(authenticateUserUseCase.authenticate(any(AuthenticateUserCommand.class)))
                    .thenReturn(tokens);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_LOGIN_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token-value"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token-value"));
        }

        @Test
        @DisplayName("returns 401 on invalid credentials")
        void returns401OnInvalidCredentials() throws Exception {
            when(authenticateUserUseCase.authenticate(any(AuthenticateUserCommand.class)))
                    .thenThrow(new InvalidCredentialsException());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_LOGIN_BODY))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("returns 403 when user is not active")
        void returns403WhenUserNotActive() throws Exception {
            when(authenticateUserUseCase.authenticate(any(AuthenticateUserCommand.class)))
                    .thenThrow(new UserNotActiveException(UserStatus.PENDING));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_LOGIN_BODY))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("PENDING")));
        }

        @Test
        @DisplayName("returns 400 when email is blank")
        void returns400WhenEmailBlank() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"","password":"p@ssw0rd"}"""))
                    .andExpect(status().isBadRequest());
        }
    }
}