package com.engine.payment.domain.model;

public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    REFUNDED,
    FAILED,
    VOIDED
}
