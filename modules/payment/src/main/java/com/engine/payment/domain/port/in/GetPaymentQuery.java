package com.engine.payment.domain.port.in;

import com.engine.payment.domain.model.GatewayReference;
import com.engine.payment.domain.model.PaymentStatus;
import com.engine.shared.domain.ids.PaymentId;
import com.engine.shared.domain.model.Money;

public interface GetPaymentQuery {
    record PaymentView(PaymentId id, PaymentStatus status, Money amount, Money capturedAmount, Money refundedAmount, GatewayReference gatewayReference) {}
    PaymentView getPayment(PaymentId id);
}
