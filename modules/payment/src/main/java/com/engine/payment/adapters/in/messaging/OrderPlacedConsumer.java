package com.engine.payment.adapters.in.messaging;

import com.engine.payment.domain.model.CardToken;
import com.engine.payment.domain.port.in.AuthorizePaymentUseCase;
import com.engine.shared.domain.ids.OrderId;
import com.engine.shared.domain.ids.PaymentId;
import com.engine.shared.domain.model.Money;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Component
public class OrderPlacedConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderPlacedConsumer.class);

    private final AuthorizePaymentUseCase authorizePaymentUseCase;
    private final ObjectMapper objectMapper;

    public OrderPlacedConsumer(AuthorizePaymentUseCase authorizePaymentUseCase, ObjectMapper objectMapper) {
        this.authorizePaymentUseCase = authorizePaymentUseCase;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "payment.order-placed")
    public void handle(String payload) {
        try {
            var node = objectMapper.readTree(payload);
            UUID eventId = UUID.fromString(node.get("eventId").asText());
            UUID orderId = UUID.fromString(node.get("aggregateId").asText());
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            String currency = "USD";
            var items = node.get("items");
            if (items != null && items.isArray()) {
                for (var item : items) {
                    BigDecimal unitPrice = new BigDecimal(item.get("unitPrice").asText());
                    int quantity = item.get("quantity").asInt();
                    totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
                    currency = item.get("currency").asText();
                }
            }

            Money amount = Money.of(totalAmount, Currency.getInstance(currency));

            String idempotencyKey = "auth-" + eventId;
            String hash = String.valueOf(payload.hashCode());

            AuthorizePaymentUseCase.AuthorizePaymentCommand cmd = new AuthorizePaymentUseCase.AuthorizePaymentCommand(
                new PaymentId(UUID.randomUUID()),
                new OrderId(orderId),
                amount,
                new CardToken("tok_system_auto"), 
                idempotencyKey,
                hash
            );

            authorizePaymentUseCase.authorize(cmd);
            log.info("Successfully handled OrderPlaced event {} and authorized payment", eventId);
        } catch (Exception e) {
            log.error("Error processing OrderPlaced event", e);
            throw new RuntimeException("Failed to process OrderPlaced", e);
        }
    }
}
