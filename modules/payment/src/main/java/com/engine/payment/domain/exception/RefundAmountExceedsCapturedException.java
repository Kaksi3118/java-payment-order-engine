package com.engine.payment.domain.exception;

import com.engine.shared.domain.ids.PaymentId;
import com.engine.shared.domain.model.Money;

public class RefundAmountExceedsCapturedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public RefundAmountExceedsCapturedException(PaymentId id, Money requested, Money available) {
        super("Refund amount " + requested.amount() + " exceeds available captured amount " + available.amount() + " for payment " + id.value());
    }
}
