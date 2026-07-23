package com.engine.payment.domain.port.out;

import com.engine.payment.domain.model.CardToken;
import com.engine.payment.domain.model.GatewayReference;
import com.engine.shared.domain.model.Money;

public interface PaymentGatewayPort {
    GatewayReference authorize(CardToken token, Money amount);
    GatewayReference capture(GatewayReference authorizationReference, Money amount);
    GatewayReference refund(GatewayReference captureReference, Money amount);
    void voidPayment(GatewayReference authorizationReference);
}
