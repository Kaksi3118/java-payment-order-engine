package com.engine.payment.domain.port.in;

import com.engine.shared.domain.ids.PaymentId;
import com.engine.shared.domain.model.Money;

public interface RefundPaymentUseCase {
    record RefundPaymentCommand(PaymentId paymentId, Money amount, String idempotencyKey, String requestHash) {}
    void refund(RefundPaymentCommand command);
}
