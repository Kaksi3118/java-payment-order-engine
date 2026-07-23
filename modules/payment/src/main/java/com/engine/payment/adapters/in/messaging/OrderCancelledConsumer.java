package com.engine.payment.adapters.in.messaging;

import com.engine.payment.domain.model.Payment;
import com.engine.payment.domain.port.in.RefundPaymentUseCase;
import com.engine.payment.domain.port.out.PaymentRepository;
import com.engine.shared.domain.ids.OrderId;
import com.engine.shared.domain.ids.PaymentId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class OrderCancelledConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderCancelledConsumer.class);

    private final RefundPaymentUseCase refundPaymentUseCase;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public OrderCancelledConsumer(RefundPaymentUseCase refundPaymentUseCase, PaymentRepository paymentRepository, ObjectMapper objectMapper) {
        this.refundPaymentUseCase = refundPaymentUseCase;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "payment.order-cancelled")
    public void handle(String payload) {
        try {
            var node = objectMapper.readTree(payload);
            UUID eventId = UUID.fromString(node.get("eventId").asText());
            UUID orderId = UUID.fromString(node.get("aggregateId").asText());

            Optional<Payment> paymentOpt = paymentRepository.findByOrderId(new OrderId(orderId));
            if (paymentOpt.isEmpty()) {
                log.warn("Received OrderCancelled for order {} but no payment found", orderId);
                return; // Nothing to refund
            }
            Payment payment = paymentOpt.get();

            // Depending on status, we might just void it or refund it. We will just refund the captured amount.
            if (payment.capturedAmount().amount().longValue() == 0) {
                log.info("Payment for order {} has not been captured, nothing to refund", orderId);
                return;
            }

            String idempotencyKey = "ref-" + eventId;
            String hash = String.valueOf(payload.hashCode());

            RefundPaymentUseCase.RefundPaymentCommand cmd = new RefundPaymentUseCase.RefundPaymentCommand(
                payment.id(),
                payment.capturedAmount(), // Refund the full captured amount
                idempotencyKey,
                hash
            );

            refundPaymentUseCase.refund(cmd);
            log.info("Successfully handled OrderCancelled event {} and refunded payment {}", eventId, payment.id().value());
        } catch (Exception e) {
            log.error("Error processing OrderCancelled event", e);
            throw new RuntimeException("Failed to process OrderCancelled", e);
        }
    }
}
