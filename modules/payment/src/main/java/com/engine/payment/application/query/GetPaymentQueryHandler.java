package com.engine.payment.application.query;

import com.engine.payment.domain.exception.PaymentNotFoundException;
import com.engine.payment.domain.model.Payment;
import com.engine.payment.domain.port.in.GetPaymentQuery;
import com.engine.payment.domain.port.out.PaymentRepository;
import com.engine.shared.domain.ids.PaymentId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPaymentQueryHandler implements GetPaymentQuery {

    private final PaymentRepository repository;

    public GetPaymentQueryHandler(PaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentView getPayment(PaymentId id) {
        Payment payment = repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        
        return new PaymentView(
                payment.id(),
                payment.status(),
                payment.amount(),
                payment.capturedAmount(),
                payment.refundedAmount(),
                payment.gatewayReference()
        );
    }
}
