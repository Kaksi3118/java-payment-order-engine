package com.engine.payment.application.service;

import com.engine.payment.domain.exception.PaymentNotFoundException;
import com.engine.payment.domain.model.GatewayReference;
import com.engine.payment.domain.model.Payment;
import com.engine.payment.domain.port.in.RefundPaymentUseCase;
import com.engine.payment.domain.port.out.IdempotencyPort;
import com.engine.payment.domain.port.out.PaymentGatewayPort;
import com.engine.payment.domain.port.out.PaymentRepository;
import com.engine.shared.domain.event.DomainEvent;
import com.engine.shared.domain.port.out.EventOutbox;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RefundPaymentService implements RefundPaymentUseCase {

    private final PaymentRepository repository;
    private final PaymentGatewayPort gateway;
    private final IdempotencyPort idempotencyPort;
    private final EventOutbox outbox;

    public RefundPaymentService(PaymentRepository repository, PaymentGatewayPort gateway, IdempotencyPort idempotencyPort, EventOutbox outbox) {
        this.repository = repository;
        this.gateway = gateway;
        this.idempotencyPort = idempotencyPort;
        this.outbox = outbox;
    }

    @Override
    @Transactional
    public void refund(RefundPaymentCommand command) {
        Optional<String> cachedResult = idempotencyPort.findResult(command.idempotencyKey(), command.requestHash());
        if (cachedResult.isPresent()) {
            return;
        }

        Payment payment = repository.findById(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

        GatewayReference refRef = gateway.refund(payment.gatewayReference(), command.amount());
        payment.refund(command.amount(), refRef);

        repository.save(payment);

        for (DomainEvent event : payment.domainEvents()) {
            outbox.append(event);
        }
        payment.clearEvents();

        idempotencyPort.saveResult(command.idempotencyKey(), command.requestHash(), "{}");
    }
}
