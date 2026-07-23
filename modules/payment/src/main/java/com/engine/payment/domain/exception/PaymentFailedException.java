package com.engine.payment.domain.exception;

import com.engine.shared.domain.ids.PaymentId;

public class PaymentFailedException extends RuntimeException {
    public PaymentFailedException(PaymentId id, String reason) {
        super("Payment " + id.value() + " failed: " + reason);
    }
}
