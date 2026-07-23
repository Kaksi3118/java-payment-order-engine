package com.engine.payment.application.service;

import com.engine.payment.domain.exception.PaymentNotFoundException;
import com.engine.payment.domain.model.GatewayReference;
import com.engine.payment.domain.model.Payment;
import com.engine.payment.domain.port.in.CapturePaymentUseCase;
import com.engine.payment.domain.port.out.PaymentGatewayPort;
import com.engine.payment.domain.port.out.PaymentRepository;
import com.engine.shared.domain.event.DomainEvent;
import com.engine.shared.domain.port.out.EventOutbox;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CapturePaymentService implements CapturePaymentUseCase {

    private final PaymentRepository repository;
    private final PaymentGatewayPort gateway;
    private final EventOutbox outbox;

    public CapturePaymentService(PaymentRepository repository, PaymentGatewayPort gateway, EventOutbox outbox) {
        this.repository = repository;
        this.gateway = gateway;
        this.outbox = outbox;
    }

    @Override
    @Transactional
    public void capture(CapturePaymentCommand command) {
        Payment payment = repository.findById(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

        GatewayReference capRef = gateway.capture(payment.gatewayReference(), payment.amount());
        payment.capture(capRef);

        repository.save(payment);

        for (DomainEvent event : payment.domainEvents()) {
            outbox.append(event);
        }
        payment.clearEvents();
    }
}
