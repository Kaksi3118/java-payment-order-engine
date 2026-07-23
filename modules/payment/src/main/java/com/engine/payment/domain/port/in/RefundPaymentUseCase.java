package com.engine.payment.domain.port.in;

import com.engine.shared.domain.ids.PaymentId;
import com.engine.shared.domain.model.Money;

public interface RefundPaymentUseCase {
    record RefundPaymentCommand(PaymentId paymentId, Money amount) {}
    void refund(RefundPaymentCommand command);
}
