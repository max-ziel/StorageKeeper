package io.github.maxziel.storagekeeper.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PantryItemTest {

    private final PantryItem rice = PantryItem.create(
            new ItemName("Reis"),
            new Quantity(BigDecimal.ONE, Unit.KILOGRAM),
            LocalDate.of(2027, 3, 1)
    );

    @Test
    void createAssignsNewIds() {
        PantryItem other = PantryItem.create(rice.getItemName(), rice.getQuantity(), null);

        assertThat(other.getPantryItemId()).isNotEqualTo(rice.getPantryItemId());
        assertThat(other.getExpiryDate()).isEmpty();
    }

    @Test
    void changeQuantityKeepsUnit() {
        rice.changeQuantity(new BigDecimal("0.5"));

        assertThat(rice.getQuantity()).isEqualTo(new Quantity(new BigDecimal("0.5"), Unit.KILOGRAM));
    }

    @Test
    void changeQuantityRejectsNegativeAmount() {
        assertThatIllegalArgumentException().isThrownBy(() -> rice.changeQuantity(new BigDecimal("-1")));
    }

    @Test
    void equalityIsBasedOnIdOnly() {
        PantryItem sameIdOtherState = new PantryItem(
                rice.getPantryItemId(),
                new ItemName("Anderer Name"),
                new Quantity(BigDecimal.TEN, Unit.GRAM),
                null
        );

        assertThat(sameIdOtherState).isEqualTo(rice).hasSameHashCodeAs(rice);
    }
}
