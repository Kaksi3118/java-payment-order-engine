package com.engine.payment.domain.port.in;

import com.engine.payment.domain.model.CardToken;
import com.engine.shared.domain.ids.OrderId;
import com.engine.shared.domain.ids.PaymentId;
import com.engine.shared.domain.model.Money;

public interface AuthorizePaymentUseCase {
    record AuthorizePaymentCommand(PaymentId paymentId, OrderId orderId, Money amount, CardToken cardToken, String idempotencyKey, String requestHash) {}
    void authorize(AuthorizePaymentCommand command);
}
