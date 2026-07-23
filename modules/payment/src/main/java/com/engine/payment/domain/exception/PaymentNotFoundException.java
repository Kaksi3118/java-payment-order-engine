package com.engine.payment.domain.exception;

import com.engine.shared.domain.ids.PaymentId;

public class PaymentNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public PaymentNotFoundException(PaymentId id) {
        super("Payment " + id.value() + " not found.");
    }
}
