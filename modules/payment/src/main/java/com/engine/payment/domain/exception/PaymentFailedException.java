package com.engine.payment.domain.exception;

import com.engine.shared.domain.ids.PaymentId;

public class PaymentFailedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PaymentFailedException(PaymentId id, String reason) {
        super("Payment " + id.value() + " failed: " + reason);
    }

    public PaymentFailedException(String reason) {
        super("Payment failed: " + reason);
    }
}
