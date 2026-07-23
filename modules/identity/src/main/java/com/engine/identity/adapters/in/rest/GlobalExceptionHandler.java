package com.engine.identity.adapters.in.rest;

import com.engine.identity.adapters.in.rest.dto.ErrorResponse;
import com.engine.identity.domain.exception.EmailAlreadyRegisteredException;
import com.engine.identity.domain.exception.InvalidCredentialsException;
import com.engine.identity.domain.exception.UserNotActiveException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler: maps domain and validation exceptions to HTTP responses.
 *
 * <p>Mapping table:
 * <table>
 *   <tr><th>Exception</th><th>HTTP Status</th><th>Rationale</th></tr>
 *   <tr><td>{@link EmailAlreadyRegisteredException}</td><td>409 Conflict</td><td>Natural-key collision</td></tr>
 *   <tr><td>{@link InvalidCredentialsException}</td><td>401 Unauthorized</td><td>Auth failed; opaque message (no user enumeration)</td></tr>
 *   <tr><td>{@link UserNotActiveException}</td><td>403 Forbidden</td><td>Credentials valid but lifecycle rejected</td></tr>
 *   <tr><td>{@link MethodArgumentNotValidException}</td><td>400 Bad Request</td><td>Bean Validation failed on request body</td></tr>
 *   <tr><td>{@link IllegalArgumentException}</td><td>400 Bad Request</td><td>Invalid enum value (e.g. unknown Role)</td></tr>
 *   <tr><td>Missing {@code Idempotency-Key} header</td><td>400 Bad Request</td><td>Required header absent</td></tr>
 * </table>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(UserNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleUserNotActive(UserNotActiveException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            org.springframework.web.bind.MissingRequestHeaderException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                "Required request header '" + ex.getHeaderName() + "' is missing");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message) {
        ErrorResponse body = new ErrorResponse(
                status.value(),
                error,
                message,
                Instant.now(clock));
        return ResponseEntity.status(status).body(body);
    }
}