package com.engine.payment.domain.event;

import com.engine.shared.domain.event.DomainEvent;
import com.engine.shared.domain.ids.PaymentId;

import java.time.Instant;
import java.util.UUID;

public record PaymentFailed(
        UUID eventId,
        Instant occurredAt,
        PaymentId paymentId,
        UUID orderId,
        String reason
) implements DomainEvent {
    public PaymentFailed(PaymentId paymentId, UUID orderId, String reason) {
        this(UUID.randomUUID(), Instant.now(), paymentId, orderId, reason);
    }

    @Override
    public UUID aggregateId() {
        return paymentId.value();
    }
}
