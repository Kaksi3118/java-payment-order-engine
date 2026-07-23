package com.engine.payment.domain.exception;

import com.engine.shared.domain.ids.PaymentId;

public class PaymentAlreadyRefundedException extends RuntimeException {
    public PaymentAlreadyRefundedException(PaymentId id) {
        super("Payment " + id.value() + " has already been fully refunded.");
    }
}
