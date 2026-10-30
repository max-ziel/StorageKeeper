package io.github.maxziel.storagekeeper.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class PantryItemTest {

    private final PantryItem rice = PantryItem.create(
            new ItemName("Reis"),
            new Quantity(BigDecimal.ONE, Unit.KILOGRAM),
            LocalDate.of(2027, 3, 1),
            StorageLocationId.DEFAULT
    );

    @Test
    void createAssignsNewIds() {
        PantryItem other = PantryItem.create(rice.getItemName(), rice.getQuantity(), null, StorageLocationId.DEFAULT);

        assertThat(other.getPantryItemId()).isNotEqualTo(rice.getPantryItemId());
        assertThat(other.getExpiryDate()).isEmpty();
    }

    @Test
    void storageLocationIsRequired() {
        assertThatNullPointerException()
                .isThrownBy(() -> PantryItem.create(rice.getItemName(), rice.getQuantity(), null, null));
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
                null,
                StorageLocationId.newId()
        );

        assertThat(sameIdOtherState).isEqualTo(rice).hasSameHashCodeAs(rice);
    }

    @Test
    void daysUntilExpiryCountsFromToday() {
        assertThat(rice.daysUntilExpiry(LocalDate.of(2027, 2, 26))).isEqualTo(3);
        assertThat(rice.daysUntilExpiry(LocalDate.of(2027, 3, 1))).isZero();
        assertThat(rice.daysUntilExpiry(LocalDate.of(2027, 3, 3))).isEqualTo(-2);
    }

    @Test
    void daysUntilExpiryNeedsAnExpiryDate() {
        PantryItem salt = PantryItem.create(new ItemName("Salz"), new Quantity(BigDecimal.ONE, Unit.PACKAGE), null,
                StorageLocationId.DEFAULT);

        assertThatIllegalStateException().isThrownBy(() -> salt.daysUntilExpiry(LocalDate.of(2027, 1, 1)));
    }
}
