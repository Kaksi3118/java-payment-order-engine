package com.engine.payment.application;

import com.engine.payment.application.query.GetPaymentQueryHandler;
import com.engine.payment.application.service.AuthorizePaymentService;
import com.engine.payment.application.service.CapturePaymentService;
import com.engine.payment.application.service.RefundPaymentService;
import com.engine.payment.domain.event.PaymentAuthorized;
import com.engine.payment.domain.event.PaymentCaptured;
import com.engine.payment.domain.exception.IdempotencyConflictException;
import com.engine.payment.domain.exception.PaymentFailedException;
import com.engine.payment.domain.model.CardToken;
import com.engine.payment.domain.model.PaymentStatus;
import com.engine.payment.domain.port.in.AuthorizePaymentUseCase.AuthorizePaymentCommand;
import com.engine.payment.domain.port.in.CapturePaymentUseCase.CapturePaymentCommand;
import com.engine.payment.domain.port.in.GetPaymentQuery.PaymentView;
import com.engine.payment.domain.port.in.RefundPaymentUseCase.RefundPaymentCommand;
import com.engine.shared.domain.ids.OrderId;
import com.engine.shared.domain.ids.PaymentId;
import com.engine.shared.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentApplicationTest {

    private FakePaymentRepository repository;
    private FakePaymentGatewayPort gateway;
    private FakeIdempotencyPort idempotencyPort;
    private FakeEventOutbox outbox;

    private AuthorizePaymentService authorizeService;
    private CapturePaymentService captureService;
    private RefundPaymentService refundService;
    private GetPaymentQueryHandler queryHandler;

    @BeforeEach
    void setUp() {
        repository = new FakePaymentRepository();
        gateway = new FakePaymentGatewayPort();
        idempotencyPort = new FakeIdempotencyPort();
        outbox = new FakeEventOutbox();

        authorizeService = new AuthorizePaymentService(repository, gateway, idempotencyPort, outbox);
        captureService = new CapturePaymentService(repository, gateway, outbox);
        refundService = new RefundPaymentService(repository, gateway, idempotencyPort, outbox);
        queryHandler = new GetPaymentQueryHandler(repository);
    }

    @Test
    void shouldAuthorizePayment() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        AuthorizePaymentCommand cmd = new AuthorizePaymentCommand(
                paymentId, new OrderId(UUID.randomUUID()), Money.of(1000L, Currency.getInstance("USD")),
                new CardToken("tok_123"), "idem-1", "hash-1"
        );

        authorizeService.authorize(cmd);

        PaymentView view = queryHandler.getPayment(paymentId);
        assertThat(view.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(outbox.getEvents()).hasSize(1);
        assertThat(outbox.getEvents().get(0)).isInstanceOf(PaymentAuthorized.class);
    }

    @Test
    void shouldBeIdempotentOnAuthorize() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        AuthorizePaymentCommand cmd = new AuthorizePaymentCommand(
                paymentId, new OrderId(UUID.randomUUID()), Money.of(1000L, Currency.getInstance("USD")),
                new CardToken("tok_123"), "idem-2", "hash-2"
        );

        authorizeService.authorize(cmd);
        authorizeService.authorize(cmd); // Should do nothing

        PaymentView view = queryHandler.getPayment(paymentId);
        assertThat(view.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(outbox.getEvents()).hasSize(1);
    }

    @Test
    void shouldThrowIdempotencyConflictOnDifferentHash() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        AuthorizePaymentCommand cmd1 = new AuthorizePaymentCommand(
                paymentId, new OrderId(UUID.randomUUID()), Money.of(1000L, Currency.getInstance("USD")),
                new CardToken("tok_123"), "idem-3", "hash-3"
        );
        authorizeService.authorize(cmd1);

        AuthorizePaymentCommand cmd2 = new AuthorizePaymentCommand(
                paymentId, new OrderId(UUID.randomUUID()), Money.of(2000L, Currency.getInstance("USD")),
                new CardToken("tok_123"), "idem-3", "hash-4"
        );

        assertThatThrownBy(() -> authorizeService.authorize(cmd2))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void shouldCapturePayment() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        authorizeService.authorize(new AuthorizePaymentCommand(
                paymentId, new OrderId(UUID.randomUUID()), Money.of(1000L, Currency.getInstance("USD")),
                new CardToken("tok_123"), "idem-cap", "hash-cap"
        ));
        outbox.clear();

        captureService.capture(new CapturePaymentCommand(paymentId));

        PaymentView view = queryHandler.getPayment(paymentId);
        assertThat(view.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(outbox.getEvents()).hasSize(1);
        assertThat(outbox.getEvents().get(0)).isInstanceOf(PaymentCaptured.class);
    }

    @Test
    void shouldThrowExceptionWhenGatewayFails() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        gateway.setFailNext(true);

        AuthorizePaymentCommand cmd = new AuthorizePaymentCommand(
                paymentId, new OrderId(UUID.randomUUID()), Money.of(1000L, Currency.getInstance("USD")),
                new CardToken("tok_123"), "idem-fail", "hash-fail"
        );

        assertThatThrownBy(() -> authorizeService.authorize(cmd))
                .isInstanceOf(PaymentFailedException.class);

        assertThat(repository.findById(paymentId)).isEmpty(); // Not saved because it failed before save
    }
}
