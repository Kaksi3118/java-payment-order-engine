package com.engine.payment.adapters.in.web;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentDto {

    public record AuthorizePaymentRequest(
            UUID paymentId,
            UUID orderId,
            BigDecimal amount,
            String currency,
            String cardToken
    ) {}

    public record CapturePaymentRequest(
            // Typically no body or maybe idempotency key, but we don't need it per requirements
    ) {}

    public record RefundPaymentRequest(
            BigDecimal amount,
            String currency
    ) {}

    public record PaymentResponse(
            UUID id,
            String status,
            BigDecimal amount,
            BigDecimal capturedAmount,
            BigDecimal refundedAmount
    ) {}
}
