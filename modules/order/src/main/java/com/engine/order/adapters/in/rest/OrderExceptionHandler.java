package com.engine.order.adapters.in.rest;

import com.engine.order.adapters.in.rest.dto.ErrorResponse;
import com.engine.order.domain.exception.IdempotencyConflictException;
import com.engine.order.domain.exception.InsufficientInventoryException;
import com.engine.order.domain.exception.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;
import java.time.Instant;

/**
 * Order-specific exception handler. Sits alongside the Identity context's
 * {@link com.engine.identity.adapters.in.rest.GlobalExceptionHandler} &mdash; each bounded
 * context owns its own handler for its own exceptions.
 */
@RestControllerAdvice(basePackages = "com.engine.order.adapters.in.rest")
public class OrderExceptionHandler {

    private final Clock clock;

    public OrderExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientInventory(InsufficientInventoryException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Conflict",
                "The order was modified by another request; please retry");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message) {
        ErrorResponse body = new ErrorResponse(status.value(), error, message, Instant.now(clock));
        return ResponseEntity.status(status).body(body);
    }
}