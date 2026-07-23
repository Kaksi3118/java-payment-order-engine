package com.engine.payment.domain.exception;

import com.engine.shared.domain.ids.PaymentId;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(PaymentId id) {
        super("Payment " + id.value() + " not found.");
    }
}
