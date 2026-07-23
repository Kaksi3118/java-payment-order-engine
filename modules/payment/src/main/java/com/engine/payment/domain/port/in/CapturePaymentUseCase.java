package com.engine.payment.domain.port.in;

import com.engine.shared.domain.ids.PaymentId;

public interface CapturePaymentUseCase {
    record CapturePaymentCommand(PaymentId paymentId) {}
    void capture(CapturePaymentCommand command);
}
