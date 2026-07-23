package com.engine.payment.application;

import com.engine.payment.domain.exception.PaymentFailedException;
import com.engine.payment.domain.model.CardToken;
import com.engine.payment.domain.model.GatewayReference;
import com.engine.payment.domain.port.out.PaymentGatewayPort;
import com.engine.shared.domain.model.Money;

import java.util.UUID;

class FakePaymentGatewayPort implements PaymentGatewayPort {

    private boolean failNext = false;

    public void setFailNext(boolean failNext) {
        this.failNext = failNext;
    }

    @Override
    public GatewayReference authorize(CardToken token, Money amount) {
        if (failNext) {
            throw new PaymentFailedException("Gateway rejection");
        }
        return new GatewayReference(UUID.randomUUID().toString());
    }

    @Override
    public GatewayReference capture(GatewayReference ref, Money amount) {
        if (failNext) {
            throw new PaymentFailedException("Capture failed");
        }
        return new GatewayReference(ref.value() + "-cap");
    }

    @Override
    public GatewayReference refund(GatewayReference ref, Money amount) {
        if (failNext) {
            throw new PaymentFailedException("Refund failed");
        }
        return new GatewayReference(ref.value() + "-ref");
    }

    @Override
    public void voidPayment(GatewayReference ref) {
        if (failNext) {
            throw new PaymentFailedException("Void failed");
        }
    }
}
