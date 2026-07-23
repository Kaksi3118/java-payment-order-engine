package com.engine.payment.domain.model;

import com.engine.payment.domain.event.PaymentAuthorized;
import com.engine.payment.domain.event.PaymentCaptured;
import com.engine.payment.domain.event.PaymentFailed;
import com.engine.payment.domain.event.PaymentRefunded;
import com.engine.payment.domain.exception.PaymentAlreadyRefundedException;
import com.engine.payment.domain.exception.RefundAmountExceedsCapturedException;
import com.engine.shared.domain.ids.OrderId;
import com.engine.shared.domain.ids.PaymentId;
import com.engine.shared.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Payment")
class PaymentTest {

    private final Currency USD = Currency.getInstance("USD");

    @Nested
    @DisplayName("when created")
    class WhenCreated {
        @Test
        void shouldBeInPendingState() {
            PaymentId id = PaymentId.random();
            OrderId orderId = OrderId.random();
            Money amount = Money.of(100L, USD);

            Payment payment = new Payment(id, orderId, amount);

            assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
            assertThat(payment.amount()).isEqualTo(amount);
            assertThat(payment.capturedAmount()).isEqualTo(Money.of(0L, USD));
            assertThat(payment.refundedAmount()).isEqualTo(Money.of(0L, USD));
            assertThat(payment.domainEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("when authorized")
    class WhenAuthorized {
        @Test
        void shouldTransitionToAuthorizedAndRegisterEvent() {
            Payment payment = new Payment(PaymentId.random(), OrderId.random(), Money.of(100L, USD));
            GatewayReference ref = new GatewayReference("auth-123");

            payment.authorize(ref);

            assertThat(payment.status()).isEqualTo(PaymentStatus.AUTHORIZED);
            assertThat(payment.gatewayReference()).isEqualTo(ref);
            assertThat(payment.domainEvents()).hasSize(1);
            assertThat(payment.domainEvents().get(0)).isInstanceOf(PaymentAuthorized.class);
        }
    }

    @Nested
    @DisplayName("when captured")
    class WhenCaptured {
        @Test
        void shouldTransitionToCapturedAndRegisterEvent() {
            Payment payment = new Payment(PaymentId.random(), OrderId.random(), Money.of(100L, USD));
            payment.authorize(new GatewayReference("auth-123"));
            payment.clearEvents();

            GatewayReference capRef = new GatewayReference("cap-123");
            payment.capture(capRef);

            assertThat(payment.status()).isEqualTo(PaymentStatus.CAPTURED);
            assertThat(payment.capturedAmount()).isEqualTo(Money.of(100L, USD));
            assertThat(payment.gatewayReference()).isEqualTo(capRef);
            assertThat(payment.domainEvents()).hasSize(1);
            assertThat(payment.domainEvents().get(0)).isInstanceOf(PaymentCaptured.class);
        }

        @Test
        void shouldFailIfNotAuthorized() {
            Payment payment = new Payment(PaymentId.random(), OrderId.random(), Money.of(100L, USD));
            
            assertThatThrownBy(() -> payment.capture(new GatewayReference("cap-123")))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("when refunded")
    class WhenRefunded {
        @Test
        void shouldTransitionToRefundedAndRegisterEvent() {
            Payment payment = new Payment(PaymentId.random(), OrderId.random(), Money.of(100L, USD));
            payment.authorize(new GatewayReference("auth-123"));
            payment.capture(new GatewayReference("cap-123"));
            payment.clearEvents();

            GatewayReference refRef = new GatewayReference("ref-123");
            payment.refund(Money.of(40L, USD), refRef);

            assertThat(payment.status()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(payment.refundedAmount()).isEqualTo(Money.of(40L, USD));
            assertThat(payment.gatewayReference()).isEqualTo(refRef);
            assertThat(payment.domainEvents()).hasSize(1);
            assertThat(payment.domainEvents().get(0)).isInstanceOf(PaymentRefunded.class);
        }

        @Test
        void shouldAllowPartialRefundsUpToCapturedAmount() {
            Payment payment = new Payment(PaymentId.random(), OrderId.random(), Money.of(100L, USD));
            payment.authorize(new GatewayReference("auth-123"));
            payment.capture(new GatewayReference("cap-123"));
            
            payment.refund(Money.of(40L, USD), new GatewayReference("ref-1"));
            payment.refund(Money.of(60L, USD), new GatewayReference("ref-2"));

            assertThat(payment.refundedAmount()).isEqualTo(Money.of(100L, USD));
        }

        @Test
        void shouldFailIfExceedsCapturedAmount() {
            Payment payment = new Payment(PaymentId.random(), OrderId.random(), Money.of(100L, USD));
            payment.authorize(new GatewayReference("auth-123"));
            payment.capture(new GatewayReference("cap-123"));
            
            assertThatThrownBy(() -> payment.refund(Money.of(150L, USD), new GatewayReference("ref-1")))
                    .isInstanceOf(RefundAmountExceedsCapturedException.class);
        }

        @Test
        void shouldFailIfAlreadyFullyRefunded() {
            Payment payment = new Payment(PaymentId.random(), OrderId.random(), Money.of(100L, USD));
            payment.authorize(new GatewayReference("auth-123"));
            payment.capture(new GatewayReference("cap-123"));
            payment.refund(Money.of(100L, USD), new GatewayReference("ref-1"));
            
            assertThatThrownBy(() -> payment.refund(Money.of(10L, USD), new GatewayReference("ref-2")))
                    .isInstanceOf(PaymentAlreadyRefundedException.class);
        }

        @Test
        void shouldFailIfNotCapturedOrRefunded() {
            Payment payment = new Payment(PaymentId.random(), OrderId.random(), Money.of(100L, USD));
            
            assertThatThrownBy(() -> payment.refund(Money.of(100L, USD), new GatewayReference("ref-1")))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("when failed")
    class WhenFailed {
        @Test
        void shouldTransitionToFailedAndRegisterEvent() {
            Payment payment = new Payment(PaymentId.random(), OrderId.random(), Money.of(100L, USD));

            payment.fail("Card declined");

            assertThat(payment.status()).isEqualTo(PaymentStatus.FAILED);
            assertThat(payment.domainEvents()).hasSize(1);
            assertThat(payment.domainEvents().get(0)).isInstanceOf(PaymentFailed.class);
        }
    }
}
