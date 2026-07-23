package com.engine.payment.adapters.out.persistence;

import com.engine.payment.domain.model.GatewayReference;
import com.engine.payment.domain.model.Payment;
import com.engine.payment.domain.model.PaymentStatus;
import com.engine.shared.domain.ids.OrderId;
import com.engine.shared.domain.ids.PaymentId;
import com.engine.shared.domain.model.Money;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.Currency;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    private UUID id;

    private UUID orderId;

    private Long amountValue;
    private String amountCurrency;

    private Long capturedValue;
    private String capturedCurrency;

    private Long refundedValue;
    private String refundedCurrency;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String gatewayReference;

    @Version
    private Long version;

    protected PaymentEntity() {}

    public static PaymentEntity fromDomain(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.id = payment.id().value();
        entity.orderId = payment.orderId().value();
        
        entity.amountValue = payment.amount().amount().longValue();
        entity.amountCurrency = payment.amount().currency().getCurrencyCode();

        entity.capturedValue = payment.capturedAmount().amount().longValue();
        entity.capturedCurrency = payment.capturedAmount().currency().getCurrencyCode();

        entity.refundedValue = payment.refundedAmount().amount().longValue();
        entity.refundedCurrency = payment.refundedAmount().currency().getCurrencyCode();

        entity.status = payment.status();
        entity.gatewayReference = payment.gatewayReference() != null ? payment.gatewayReference().value() : null;
        return entity;
    }

    public Payment toDomain() {
        Money amount = Money.of(amountValue, Currency.getInstance(amountCurrency));
        Money captured = Money.of(capturedValue, Currency.getInstance(capturedCurrency));
        Money refunded = Money.of(refundedValue, Currency.getInstance(refundedCurrency));
        GatewayReference ref = gatewayReference != null ? new GatewayReference(gatewayReference) : null;

        return Payment.reconstitute(
                new PaymentId(id),
                new OrderId(orderId),
                amount,
                status,
                captured,
                refunded,
                ref
        );
    }
}
