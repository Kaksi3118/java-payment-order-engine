package com.engine.bootstrap;

import com.engine.bootstrap.config.RabbitMQConfig;
import com.engine.identity.adapters.out.persistence.OutboxEntity;
import com.engine.identity.adapters.out.persistence.OutboxJpaRepository;
import com.engine.identity.adapters.out.persistence.OutboxStatus;
import com.engine.shared.domain.ids.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class, properties = {
        "spring.rabbitmq.listener.simple.auto-startup=true"
})
@Testcontainers
@EnabledIfEnvironmentVariable(named = "DOCKER_HOST", matches = ".*")
@DisplayName("RabbitMQ Wiring & Outbox Poller Integration Test")
class RabbitMQIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("poe_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withExposedPorts(5672, 15672);

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private OutboxJpaRepository outboxJpaRepository;

    @Autowired
    private com.engine.bootstrap.outbox.OutboxPoller outboxPoller;

    @BeforeEach
    void setup() {
        outboxJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("OutboxPoller leases PENDING rows, publishes to RabbitMQ, and marks PUBLISHED")
    void testOutboxPoller() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        String eventType = "com.engine.order.domain.event.OrderPlaced";
        String payload = """
                {
                  "eventId": "%s",
                  "aggregateId": "%s",
                  "customerId": "%s",
                  "items": [
                    {
                      "productId": "%s",
                      "quantity": 1,
                      "unitPrice": 10.00,
                      "currency": "USD"
                    }
                  ],
                  "occurredAt": "%s"
                }
                """.formatted(eventId, aggregateId, UUID.randomUUID(), UUID.randomUUID(), Instant.now());

        OutboxEntity entity = new OutboxEntity(eventId, aggregateId, eventType, payload, OutboxStatus.PENDING, Instant.now(), Instant.now());
        outboxJpaRepository.save(entity);

        // Act
        outboxPoller.pollOutbox(); // manually invoke to avoid waiting for @Scheduled

        // Assert
        OutboxEntity processed = outboxJpaRepository.findById(eventId).orElseThrow();
        assertThat(processed.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }
}
