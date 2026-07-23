package com.engine.payment.domain.event;

import com.engine.shared.domain.event.DomainEvent;
import com.engine.shared.domain.ids.PaymentId;
import com.engine.shared.domain.model.Money;

import java.time.Instant;
import java.util.UUID;

public record PaymentCaptured(
        UUID eventId,
        Instant occurredAt,
        PaymentId paymentId,
        UUID orderId,
        Money amount
) implements DomainEvent {
    public PaymentCaptured(PaymentId paymentId, UUID orderId, Money amount) {
        this(UUID.randomUUID(), Instant.now(), paymentId, orderId, amount);
    }

    @Override
    public UUID aggregateId() {
        return paymentId.value();
    }
}
