package com.engine.shared.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money value object")
class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency JPY = Currency.getInstance("JPY");

    @Nested
    @DisplayName("construction and scale normalization")
    class Construction {

        @Test
        @DisplayName("normalizes amount scale to the currency's default fraction digits")
        void normalizesScaleToCurrency() {
            Money usd = Money.of(new BigDecimal("10"), USD);
            assertThat(usd.amount().scale()).isEqualTo(USD.getDefaultFractionDigits());
            assertThat(usd.amount().toPlainString()).isEqualTo("10.00");

            Money jpy = Money.of(new BigDecimal("1000"), JPY);
            assertThat(jpy.amount().scale()).isEqualTo(JPY.getDefaultFractionDigits());
            assertThat(jpy.amount().toPlainString()).isEqualTo("1000");
        }

        @Test
        @DisplayName("two amounts with the same logical value are equal despite differing input scale")
        void equalityAfterScaleNormalization() {
            Money a = Money.of(new BigDecimal("10"), USD);
            Money b = Money.of(new BigDecimal("10.00"), USD);
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("rejects null amount and null currency")
        void rejectsNulls() {
            assertThatNullPointerException().isThrownBy(() -> Money.of((BigDecimal) null, USD));
            assertThatNullPointerException().isThrownBy(() -> Money.of(BigDecimal.ZERO, (Currency) null));
            assertThatNullPointerException().isThrownBy(() -> Money.of((BigDecimal) null, "USD"));
        }

        @Test
        @DisplayName("parses ISO 4217 codes via factory")
        void parsesCurrencyCode() {
            Money m = Money.of(BigDecimal.TEN, "EUR");
            assertThat(m.currency()).isEqualTo(EUR);
        }

        @Test
        @DisplayName("zero factory yields a zero amount")
        void zeroIsZero() {
            Money zero = Money.zero(USD);
            assertThat(zero.isZero()).isTrue();
            assertThat(zero.amount().scale()).isEqualTo(USD.getDefaultFractionDigits());
        }
    }

    @Nested
    @DisplayName("rounding behavior")
    class Rounding {

        @Test
        @DisplayName("applies HALF_EVEN to ties: even neighbor wins")
        void roundsTiesToEven() {
            // 5.005 -> scale 2: hundredths 0 (even neighbor) wins -> 5.00
            Money lower = Money.of(new BigDecimal("5.005"), USD);
            assertThat(lower.amount()).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(lower.amount().scale()).isEqualTo(2);

            // 5.015 -> scale 2: hundredths 1 (odd) -> up to even 2 -> 5.02
            Money upper = Money.of(new BigDecimal("5.015"), USD);
            assertThat(upper.amount()).isEqualByComparingTo(new BigDecimal("5.02"));
            assertThat(upper.amount().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("multiplication by a fractional factor re-scales using HALF_EVEN")
        void multiplicationRoundsToCurrencyScale() {
            Money thirtyCents = Money.of(new BigDecimal("0.35"), USD);
            // 0.35 * 0.5 = 0.175 -> scale 2 HALF_EVEN -> 0.18 (hundredths 7 odd -> up to even 8)
            Money result = thirtyCents.multiplyBy(new BigDecimal("0.5"));
            assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("0.18"));
            assertThat(result.amount().scale()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("arithmetic")
    class Arithmetic {

        @Test
        @DisplayName("plus and minus preserve currency and value")
        void addAndSubtract() {
            Money ten = Money.of(new BigDecimal("10"), USD);
            Money five = Money.of(new BigDecimal("5"), USD);
            assertThat(ten.plus(five)).isEqualTo(Money.of(new BigDecimal("15"), USD));
            assertThat(ten.minus(five)).isEqualTo(Money.of(new BigDecimal("5"), USD));
        }

        @Test
        @DisplayName("multiplication works for int and BigDecimal factors")
        void multiplication() {
            Money ten = Money.of(new BigDecimal("10"), USD);
            assertThat(ten.multiplyBy(2)).isEqualTo(Money.of(new BigDecimal("20"), USD));
            assertThat(ten.multiplyBy(new BigDecimal("0.1")))
                    .isEqualByComparingTo(Money.of(new BigDecimal("1"), USD));
        }

        @Test
        @DisplayName("negate and abs work as expected")
        void negateAndAbs() {
            Money minusTen = Money.of(new BigDecimal("-10"), USD);
            assertThat(minusTen.negate()).isEqualTo(Money.of(new BigDecimal("10"), USD));
            assertThat(minusTen.abs()).isEqualTo(Money.of(new BigDecimal("10"), USD));
            assertThat(minusTen.abs().isPositive()).isTrue();
        }

        @Test
        @DisplayName("binary operations reject mismatched currencies with a typed exception")
        void rejectsCurrencyMismatch() {
            Money tenUsd = Money.of(BigDecimal.TEN, USD);
            Money tenEur = Money.of(BigDecimal.TEN, EUR);
            assertThatThrownBy(() -> tenUsd.plus(tenEur)).isInstanceOf(CurrencyMismatchException.class);
            assertThatThrownBy(() -> tenUsd.minus(tenEur)).isInstanceOf(CurrencyMismatchException.class);
            assertThatThrownBy(() -> tenUsd.compareTo(tenEur)).isInstanceOf(CurrencyMismatchException.class);
            assertThatThrownBy(() -> tenUsd.isGreaterThan(tenEur)).isInstanceOf(CurrencyMismatchException.class);
        }
    }

    @Nested
    @DisplayName("predicates and comparison")
    class Predicates {

        @Test
        void signPredicates() {
            Money zero = Money.zero(USD);
            assertThat(zero.isZero()).isTrue();
            assertThat(zero.isPositive()).isFalse();
            assertThat(zero.isNegative()).isFalse();
            assertThat(Money.of(new BigDecimal("-5"), USD).isNegative()).isTrue();
            assertThat(Money.of(new BigDecimal("5"), USD).isPositive()).isTrue();
        }

        @Test
        void comparisons() {
            Money ten = Money.of(new BigDecimal("10"), USD);
            Money twenty = Money.of(new BigDecimal("20"), USD);
            assertThat(ten.isLessThan(twenty)).isTrue();
            assertThat(twenty.isGreaterThan(ten)).isTrue();
            assertThat(ten.isLessThanOrEqualTo(twenty)).isTrue();
            assertThat(ten.isLessThanOrEqualTo(Money.of(new BigDecimal("10"), USD))).isTrue();
            assertThat(twenty.isGreaterThanOrEqualTo(ten)).isTrue();
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {
        @Test
        void includesAmountAndCurrencyCode() {
            Money m = Money.of(new BigDecimal("12.50"), "USD");
            assertThat(m).hasToString("12.50 USD");
        }
    }
}