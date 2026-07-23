package com.engine.payment.application;

import com.engine.payment.domain.model.Payment;
import com.engine.payment.domain.port.out.PaymentRepository;
import com.engine.shared.domain.ids.PaymentId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class FakePaymentRepository implements PaymentRepository {

    private final Map<PaymentId, Payment> store = new ConcurrentHashMap<>();

    @Override
    public void save(Payment payment) {
        store.put(payment.id(), payment);
    }

    @Override
    public Optional<Payment> findById(PaymentId id) {
        return Optional.ofNullable(store.get(id));
    }
    
    public void clear() {
        store.clear();
    }
}
