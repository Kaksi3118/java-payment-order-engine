package com.engine.payment.domain.exception;

import com.engine.shared.domain.ids.PaymentId;
import com.engine.shared.domain.model.Money;

public class RefundAmountExceedsCapturedException extends RuntimeException {
    public RefundAmountExceedsCapturedException(PaymentId id, Money requested, Money available) {
        super("Refund amount " + requested.amount() + " exceeds available captured amount " + available.amount() + " for payment " + id.value());
    }
}
