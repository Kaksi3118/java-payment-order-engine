package com.engine.order.adapters.in.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserRegisteredConsumer {
    private static final Logger log = LoggerFactory.getLogger(UserRegisteredConsumer.class);

    private final ObjectMapper objectMapper;

    public UserRegisteredConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "order.user-registered")
    public void handle(String payload) {
        try {
            var node = objectMapper.readTree(payload);
            UUID eventId = UUID.fromString(node.get("eventId").asText());
            UUID userId = UUID.fromString(node.get("aggregateId").asText());
            String email = node.get("email").get("value").asText();

            // Here we would typically update a local projection or Customer record.
            // For now, we simply log the event.
            log.info("Successfully handled UserRegistered event {}. Registered user: {} with email {}", eventId, userId, email);
        } catch (Exception e) {
            log.error("Error processing UserRegistered event", e);
            throw new RuntimeException("Failed to process UserRegistered", e);
        }
    }
}
