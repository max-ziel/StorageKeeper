package io.github.maxziel.storagekeeper.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class QuantityTest {

    @Test
    void allowZero() {
        assertThat(new Quantity(BigDecimal.ZERO, Unit.PIECE).amount()).isZero();
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Quantity(new BigDecimal("-1"), Unit.GRAM));
    }

    @Test
    void withAmountKeepsUnit() {
        Quantity changed = new Quantity(BigDecimal.ZERO, Unit.LITER).withAmount(new BigDecimal("2.5"));

        assertThat(changed).isEqualTo(new Quantity(new BigDecimal("2.5"), Unit.LITER));
    }
}
