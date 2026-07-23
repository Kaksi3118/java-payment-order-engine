package com.engine.bootstrap.outbox;

import com.engine.bootstrap.config.RabbitMQConfig;
import com.engine.identity.adapters.out.persistence.OutboxEntity;
import com.engine.identity.adapters.out.persistence.OutboxJpaRepository;
import com.engine.identity.adapters.out.persistence.OutboxStatus;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@EnableScheduling
@Configuration
public class OutboxPoller {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxJpaRepository outboxJpaRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RedissonClient redissonClient;

    public OutboxPoller(OutboxJpaRepository outboxJpaRepository, RabbitTemplate rabbitTemplate, RedissonClient redissonClient) {
        this.outboxJpaRepository = outboxJpaRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.redissonClient = redissonClient;
    }

    @Scheduled(fixedDelay = 5000)
    public void pollOutbox() {
        RLock lock = redissonClient.getLock("outbox-dispatcher");
        try {
            if (lock.tryLock(0, 4, TimeUnit.SECONDS)) {
                try {
                    processPendingRows();
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Transactional
    public void processPendingRows() {
        List<OutboxEntity> pendingEvents = outboxJpaRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEntity event : pendingEvents) {
            String exchange = resolveExchange(event.getEventType());
            String routingKey = event.getEventType();

            log.info("Publishing event {} to exchange {} with routing key {}", event.getId(), exchange, routingKey);
            
            // Set message properties like content_type to application/json if needed,
            // but we can just send the string.
            rabbitTemplate.convertAndSend(exchange, routingKey, event.getPayload());
            
            event.markPublished();
            outboxJpaRepository.save(event);
        }
    }

    private String resolveExchange(String eventType) {
        if (eventType.startsWith("com.engine.order.")) {
            return RabbitMQConfig.ORDERS_EXCHANGE;
        } else if (eventType.startsWith("com.engine.payment.")) {
            return RabbitMQConfig.PAYMENTS_EXCHANGE;
        } else if (eventType.startsWith("com.engine.identity.")) {
            return RabbitMQConfig.IDENTITY_EXCHANGE;
        }
        throw new IllegalArgumentException("Unknown event type: " + eventType);
    }
}
