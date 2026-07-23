package com.engine.payment.domain.exception;

import com.engine.shared.domain.ids.PaymentId;

public class PaymentAlreadyRefundedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public PaymentAlreadyRefundedException(PaymentId id) {
        super("Payment " + id.value() + " has already been fully refunded.");
    }
}
