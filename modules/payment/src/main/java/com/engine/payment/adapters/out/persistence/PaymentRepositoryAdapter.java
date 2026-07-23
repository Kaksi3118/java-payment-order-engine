package com.engine.payment.adapters.out.persistence;

import com.engine.payment.domain.model.Payment;
import com.engine.payment.domain.port.out.PaymentRepository;
import com.engine.shared.domain.ids.PaymentId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final SpringDataPaymentRepository springDataRepo;

    public PaymentRepositoryAdapter(SpringDataPaymentRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public void save(Payment payment) {
        springDataRepo.save(PaymentEntity.fromDomain(payment));
    }

    @Override
    public Optional<Payment> findById(PaymentId id) {
        return springDataRepo.findById(id.value()).map(PaymentEntity::toDomain);
    }
}
