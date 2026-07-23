package com.engine.payment.domain.model;

import com.engine.payment.domain.event.PaymentAuthorized;
import com.engine.payment.domain.event.PaymentCaptured;
import com.engine.payment.domain.event.PaymentFailed;
import com.engine.payment.domain.event.PaymentRefunded;
import com.engine.payment.domain.exception.PaymentAlreadyRefundedException;
import com.engine.payment.domain.exception.RefundAmountExceedsCapturedException;
import com.engine.shared.domain.ids.OrderId;
import com.engine.shared.domain.ids.PaymentId;
import com.engine.shared.domain.model.AggregateRoot;
import com.engine.shared.domain.model.Money;

import java.util.Objects;

public class Payment extends AggregateRoot {
    private final PaymentId id;
    private final OrderId orderId;
    private final Money amount;
    private PaymentStatus status;
    private Money capturedAmount;
    private Money refundedAmount;
    private GatewayReference gatewayReference;

    public Payment(PaymentId id, OrderId orderId, Money amount) {
        this.id = Objects.requireNonNull(id);
        this.orderId = Objects.requireNonNull(orderId);
        this.amount = Objects.requireNonNull(amount);
        this.status = PaymentStatus.PENDING;
        this.capturedAmount = Money.of(0L, amount.currency());
        this.refundedAmount = Money.of(0L, amount.currency());
    }

    private Payment(PaymentId id, OrderId orderId, Money amount, PaymentStatus status, Money capturedAmount, Money refundedAmount, GatewayReference gatewayReference) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.capturedAmount = capturedAmount;
        this.refundedAmount = refundedAmount;
        this.gatewayReference = gatewayReference;
    }

    public static Payment reconstitute(PaymentId id, OrderId orderId, Money amount, PaymentStatus status, Money capturedAmount, Money refundedAmount, GatewayReference gatewayReference) {
        return new Payment(id, orderId, amount, status, capturedAmount, refundedAmount, gatewayReference);
    }

    public void authorize(GatewayReference gatewayReference) {
        this.status = PaymentStatus.AUTHORIZED;
        this.gatewayReference = gatewayReference;
        raise(new PaymentAuthorized(this.id, this.orderId.value(), this.amount));
    }

    public void capture(GatewayReference gatewayReference) {
        if (this.status != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Payment must be AUTHORIZED to be CAPTURED");
        }
        this.status = PaymentStatus.CAPTURED;
        this.capturedAmount = this.amount;
        this.gatewayReference = gatewayReference;
        raise(new PaymentCaptured(this.id, this.orderId.value(), this.capturedAmount));
    }

    public void refund(Money refundAmount, GatewayReference gatewayReference) {
        if (this.status != PaymentStatus.CAPTURED && this.status != PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Payment must be CAPTURED to be REFUNDED");
        }
        if (this.status == PaymentStatus.REFUNDED && this.refundedAmount.equals(this.capturedAmount)) {
            throw new PaymentAlreadyRefundedException(this.id);
        }
        Money newRefundedAmount = this.refundedAmount.plus(refundAmount);
        if (newRefundedAmount.amount().compareTo(this.capturedAmount.amount()) > 0) {
            throw new RefundAmountExceedsCapturedException(this.id, refundAmount, this.capturedAmount.minus(this.refundedAmount));
        }

        this.refundedAmount = newRefundedAmount;
        this.status = PaymentStatus.REFUNDED;
        this.gatewayReference = gatewayReference;
        raise(new PaymentRefunded(this.id, this.orderId.value(), refundAmount));
    }

    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        raise(new PaymentFailed(this.id, this.orderId.value(), reason));
    }

    public PaymentId id() { return id; }
    public OrderId orderId() { return orderId; }
    public Money amount() { return amount; }
    public PaymentStatus status() { return status; }
    public Money capturedAmount() { return capturedAmount; }
    public Money refundedAmount() { return refundedAmount; }
    public GatewayReference gatewayReference() { return gatewayReference; }
}
