package com.engine.bootstrap.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDERS_EXCHANGE = "orders.events";
    public static final String PAYMENTS_EXCHANGE = "payments.events";
    public static final String IDENTITY_EXCHANGE = "identity.events";
    public static final String DLX_EXCHANGE = "dlx";

    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(ORDERS_EXCHANGE);
    }

    @Bean
    public TopicExchange paymentsExchange() {
        return new TopicExchange(PAYMENTS_EXCHANGE);
    }

    @Bean
    public TopicExchange identityExchange() {
        return new TopicExchange(IDENTITY_EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    // Queues
    @Bean
    public Queue paymentOrderPlacedQueue() {
        return QueueBuilder.durable("payment.order-placed")
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "payment.order-placed.dlq")
                .build();
    }

    @Bean
    public Queue paymentOrderPlacedDlq() {
        return QueueBuilder.durable("payment.order-placed.dlq").build();
    }

    @Bean
    public Binding paymentOrderPlacedBinding() {
        return BindingBuilder.bind(paymentOrderPlacedQueue()).to(ordersExchange()).with("com.engine.order.domain.event.OrderPlaced");
    }

    @Bean
    public Binding paymentOrderPlacedDlqBinding() {
        return BindingBuilder.bind(paymentOrderPlacedDlq()).to(deadLetterExchange()).with("payment.order-placed.dlq");
    }

    @Bean
    public Queue paymentOrderCancelledQueue() {
        return QueueBuilder.durable("payment.order-cancelled")
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "payment.order-cancelled.dlq")
                .build();
    }

    @Bean
    public Queue paymentOrderCancelledDlq() {
        return QueueBuilder.durable("payment.order-cancelled.dlq").build();
    }

    @Bean
    public Binding paymentOrderCancelledBinding() {
        return BindingBuilder.bind(paymentOrderCancelledQueue()).to(ordersExchange()).with("com.engine.order.domain.event.OrderCancelled");
    }

    @Bean
    public Binding paymentOrderCancelledDlqBinding() {
        return BindingBuilder.bind(paymentOrderCancelledDlq()).to(deadLetterExchange()).with("payment.order-cancelled.dlq");
    }

    @Bean
    public Queue orderUserRegisteredQueue() {
        return QueueBuilder.durable("order.user-registered")
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "order.user-registered.dlq")
                .build();
    }

    @Bean
    public Queue orderUserRegisteredDlq() {
        return QueueBuilder.durable("order.user-registered.dlq").build();
    }

    @Bean
    public Binding orderUserRegisteredBinding() {
        return BindingBuilder.bind(orderUserRegisteredQueue()).to(identityExchange()).with("com.engine.identity.domain.event.UserRegistered");
    }

    @Bean
    public Binding orderUserRegisteredDlqBinding() {
        return BindingBuilder.bind(orderUserRegisteredDlq()).to(deadLetterExchange()).with("order.user-registered.dlq");
    }
}
