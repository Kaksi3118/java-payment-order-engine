package com.engine.payment.adapters.out.persistence;

import com.engine.payment.domain.model.Payment;
import com.engine.payment.domain.port.out.PaymentRepository;
import com.engine.shared.domain.ids.PaymentId;
import com.engine.shared.domain.ids.OrderId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final SpringDataPaymentRepository repository;

    public PaymentRepositoryAdapter(SpringDataPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Payment payment) {
        PaymentEntity entity = PaymentEntity.fromDomain(payment);
        repository.save(entity);
    }

    @Override
    public Optional<Payment> findById(PaymentId id) {
        return repository.findById(id.value())
                .map(PaymentEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderId(OrderId orderId) {
        return repository.findByOrderId(orderId.value())
                .map(PaymentEntity::toDomain);
    }
}
