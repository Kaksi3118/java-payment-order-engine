package com.engine.shared.domain.ids;

import com.engine.shared.domain.model.Identifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Typed identifiers")
class IdentifierTest {

    @Nested
    @DisplayName("OrderId")
    class OrderIdContract {
        @Test
        void holdsProvidedUuid() {
            UUID id = UUID.randomUUID();
            assertThat(new OrderId(id).value()).isEqualTo(id);
        }

        @Test
        void nullValueIsRejected() {
            assertThatNullPointerException().isThrownBy(() -> new OrderId(null));
        }

        @Test
        void ofLiteralParsesUuid() {
            UUID id = UUID.randomUUID();
            assertThat(OrderId.of(id.toString())).isEqualTo(new OrderId(id));
        }

        @Test
        void ofLiteralRejectsInvalidFormat() {
            assertThatThrownBy(() -> OrderId.of("not-a-uuid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void randomReturnsDistinctIds() {
            assertThat(OrderId.random()).isNotEqualTo(OrderId.random());
        }

        @Test
        void implementsIdentifierContract() {
            assertThat(new OrderId(UUID.randomUUID())).isInstanceOf(Identifier.class);
        }
    }

    @Nested
    @DisplayName("PaymentId")
    class PaymentIdContract {
        @Test
        void randomProducesDistinct() {
            assertThat(PaymentId.random()).isNotEqualTo(PaymentId.random());
        }

        @Test
        void implementsIdentifierContract() {
            assertThat(new PaymentId(UUID.randomUUID())).isInstanceOf(Identifier.class);
        }
    }

    @Nested
    @DisplayName("UserId")
    class UserIdContract {
        @Test
        void randomProducesDistinct() {
            assertThat(UserId.random()).isNotEqualTo(UserId.random());
        }

        @Test
        void implementsIdentifierContract() {
            assertThat(new UserId(UUID.randomUUID())).isInstanceOf(Identifier.class);
        }
    }

    @Nested
    @DisplayName("TransactionId")
    class TransactionIdContract {
        @Test
        void randomProducesDistinct() {
            assertThat(TransactionId.random()).isNotEqualTo(TransactionId.random());
        }

        @Test
        void implementsIdentifierContract() {
            assertThat(new TransactionId(UUID.randomUUID())).isInstanceOf(Identifier.class);
        }
    }
}