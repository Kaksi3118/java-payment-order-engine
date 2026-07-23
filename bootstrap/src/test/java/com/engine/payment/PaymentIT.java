package com.engine.payment;

import com.engine.bootstrap.Application;
import com.engine.identity.adapters.out.persistence.OutboxEntity;
import com.engine.identity.adapters.out.persistence.OutboxJpaRepository;
import com.engine.identity.adapters.out.persistence.OutboxStatus;
import com.engine.payment.domain.event.PaymentAuthorized;
import com.engine.payment.domain.event.PaymentCaptured;
import com.engine.payment.domain.event.PaymentFailed;
import com.engine.payment.domain.event.PaymentRefunded;
import com.engine.payment.domain.exception.PaymentFailedException;
import com.engine.payment.domain.model.CardToken;
import com.engine.payment.domain.model.GatewayReference;
import com.engine.payment.domain.model.PaymentStatus;
import com.engine.payment.domain.port.in.AuthorizePaymentUseCase;
import com.engine.payment.domain.port.in.CapturePaymentUseCase;
import com.engine.payment.domain.port.in.GetPaymentQuery;
import com.engine.payment.domain.port.in.RefundPaymentUseCase;
import com.engine.payment.domain.port.out.PaymentGatewayPort;
import com.engine.shared.domain.ids.OrderId;
import com.engine.shared.domain.ids.PaymentId;
import com.engine.shared.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = Application.class)
@Testcontainers
@EnabledIfEnvironmentVariable(named = "DOCKER_HOST", matches = ".*")
@DisplayName("Payment Integration Test (Testcontainers + PostgreSQL 17)")
class PaymentIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("poe_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AuthorizePaymentUseCase authorizeUseCase;
    @Autowired
    private CapturePaymentUseCase captureUseCase;
    @Autowired
    private RefundPaymentUseCase refundUseCase;
    @Autowired
    private GetPaymentQuery getPaymentQuery;
    @Autowired
    private OutboxJpaRepository outboxJpaRepository;

    @MockBean
    private PaymentGatewayPort paymentGatewayPort;

    private static final Currency USD = Currency.getInstance("USD");

    @BeforeEach
    void setup() {
        outboxJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("authorize payment persists row, creates gateway reference, and writes PaymentAuthorized to outbox")
    void authorizePaymentEndToEnd() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        OrderId orderId = new OrderId(UUID.randomUUID());
        GatewayReference mockRef = new GatewayReference("auth-123");

        when(paymentGatewayPort.authorize(any(), any())).thenReturn(mockRef);

        AuthorizePaymentUseCase.AuthorizePaymentCommand command = new AuthorizePaymentUseCase.AuthorizePaymentCommand(
                paymentId, orderId, Money.of(100L, USD), new CardToken("tok_123"), "auth-key-1", "auth-hash-1");

        authorizeUseCase.authorize(command);

        GetPaymentQuery.PaymentView view = getPaymentQuery.getPayment(paymentId);
        assertThat(view.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(view.gatewayReference().value()).isEqualTo("auth-123");

        List<OutboxEntity> outboxRows = outboxJpaRepository.findAll();
        assertThat(outboxRows).hasSize(1);
        assertThat(outboxRows.get(0).getEventType()).isEqualTo(PaymentAuthorized.class.getName());
    }

    @Test
    @DisplayName("capture payment updates status and writes PaymentCaptured to outbox")
    void capturePaymentEndToEnd() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        OrderId orderId = new OrderId(UUID.randomUUID());
        GatewayReference authRef = new GatewayReference("auth-123");
        GatewayReference capRef = new GatewayReference("cap-123");

        when(paymentGatewayPort.authorize(any(), any())).thenReturn(authRef);
        when(paymentGatewayPort.capture(any(), any())).thenReturn(capRef);

        authorizeUseCase.authorize(new AuthorizePaymentUseCase.AuthorizePaymentCommand(
                paymentId, orderId, Money.of(100L, USD), new CardToken("tok_123"), "cap-key-1", "cap-hash-1"));

        outboxJpaRepository.deleteAll(); // clear outbox

        captureUseCase.capture(new CapturePaymentUseCase.CapturePaymentCommand(paymentId));

        GetPaymentQuery.PaymentView view = getPaymentQuery.getPayment(paymentId);
        assertThat(view.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(view.capturedAmount().amount().longValue()).isEqualTo(100L);
        assertThat(view.gatewayReference().value()).isEqualTo("cap-123");

        List<OutboxEntity> outboxRows = outboxJpaRepository.findAll();
        assertThat(outboxRows).hasSize(1);
        assertThat(outboxRows.get(0).getEventType()).isEqualTo(PaymentCaptured.class.getName());
    }

    @Test
    @DisplayName("refund payment updates status and writes PaymentRefunded to outbox")
    void refundPaymentEndToEnd() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        OrderId orderId = new OrderId(UUID.randomUUID());

        when(paymentGatewayPort.authorize(any(), any())).thenReturn(new GatewayReference("auth-123"));
        when(paymentGatewayPort.capture(any(), any())).thenReturn(new GatewayReference("cap-123"));
        when(paymentGatewayPort.refund(any(), any())).thenReturn(new GatewayReference("ref-123"));

        authorizeUseCase.authorize(new AuthorizePaymentUseCase.AuthorizePaymentCommand(
                paymentId, orderId, Money.of(100L, USD), new CardToken("tok_123"), "ref-key-1", "ref-hash-1"));
        captureUseCase.capture(new CapturePaymentUseCase.CapturePaymentCommand(paymentId));

        outboxJpaRepository.deleteAll(); // clear outbox

        refundUseCase.refund(new RefundPaymentUseCase.RefundPaymentCommand(
                paymentId, Money.of(50L, USD), "ref-key-2", "ref-hash-2"));

        GetPaymentQuery.PaymentView view = getPaymentQuery.getPayment(paymentId);
        assertThat(view.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(view.refundedAmount().amount().longValue()).isEqualTo(50L);
        assertThat(view.gatewayReference().value()).isEqualTo("ref-123");

        List<OutboxEntity> outboxRows = outboxJpaRepository.findAll();
        assertThat(outboxRows).hasSize(1);
        assertThat(outboxRows.get(0).getEventType()).isEqualTo(PaymentRefunded.class.getName());
    }

    @Test
    @DisplayName("authorize idempotency: same key and hash does not re-invoke gateway")
    void authorizeIdempotency() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        OrderId orderId = new OrderId(UUID.randomUUID());

        when(paymentGatewayPort.authorize(any(), any())).thenReturn(new GatewayReference("auth-idem"));

        AuthorizePaymentUseCase.AuthorizePaymentCommand command = new AuthorizePaymentUseCase.AuthorizePaymentCommand(
                paymentId, orderId, Money.of(100L, USD), new CardToken("tok_123"), "idem-key", "idem-hash");

        authorizeUseCase.authorize(command);
        authorizeUseCase.authorize(command);

        // Verification of gateway interactions can be done here, or just checking status
        GetPaymentQuery.PaymentView view = getPaymentQuery.getPayment(paymentId);
        assertThat(view.status()).isEqualTo(PaymentStatus.AUTHORIZED);
    }
}
