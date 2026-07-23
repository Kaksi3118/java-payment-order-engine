package com.engine.payment.adapters.in.web;

import com.engine.payment.domain.exception.IdempotencyConflictException;
import com.engine.payment.domain.exception.PaymentAlreadyRefundedException;
import com.engine.payment.domain.exception.PaymentFailedException;
import com.engine.payment.domain.exception.PaymentNotFoundException;
import com.engine.payment.domain.exception.RefundAmountExceedsCapturedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.engine.payment.adapters.in.web")
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<Map<String, String>> handlePaymentFailed(PaymentFailedException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handlePaymentNotFound(PaymentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(RefundAmountExceedsCapturedException.class)
    public ResponseEntity<Map<String, String>> handleRefundExceedsCaptured(RefundAmountExceedsCapturedException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(PaymentAlreadyRefundedException.class)
    public ResponseEntity<Map<String, String>> handleAlreadyRefunded(PaymentAlreadyRefundedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<Map<String, String>> handleIdempotencyConflict(IdempotencyConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<Map<String, String>> handleWebClientError(WebClientRequestException e) {
        // e.g. circuit breaker open
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "Payment gateway is unavailable"));
    }
}
