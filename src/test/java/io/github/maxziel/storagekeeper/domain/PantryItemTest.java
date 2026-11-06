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

    @Test
    void newItemHasNoMinimum() {
        assertThat(rice.getMinimumQuantity()).isEqualTo(new Quantity(BigDecimal.ZERO, Unit.KILOGRAM));
        assertThat(rice.isBelowMinimum()).isFalse();
    }

    @Test
    void isBelowMinimumOnlyWhenAmountIsLess() {
        rice.changeMinimumAmount(new BigDecimal("2"));
        assertThat(rice.isBelowMinimum()).isTrue();

        rice.changeQuantity(new BigDecimal("2.0"));
        assertThat(rice.isBelowMinimum()).isFalse();
    }

    @Test
    void minimumUsesTheItemsUnit() {
        rice.changeMinimumAmount(new BigDecimal("0.5"));

        assertThat(rice.getMinimumQuantity()).isEqualTo(new Quantity(new BigDecimal("0.5"), Unit.KILOGRAM));
    }

    @Test
    void changeMinimumAmountRejectsNegativeAmount() {
        assertThatIllegalArgumentException().isThrownBy(() -> rice.changeMinimumAmount(new BigDecimal("-1")));
    }

    @Test
    void fallingBelowMinimumRaisesEventOnce() {
        rice.changeMinimumAmount(new BigDecimal("2"));
        rice.changeQuantity(new BigDecimal("0.5"));

        assertThat(rice.pullDomainEvents()).containsExactly(new PantryItemFellBelowMinimum(rice.getPantryItemId()));
        assertThat(rice.pullDomainEvents()).isEmpty();
    }

    @Test
    void reachingMinimumAgainRaisesEvent() {
        rice.changeMinimumAmount(new BigDecimal("2"));
        rice.pullDomainEvents();

        rice.changeQuantity(new BigDecimal("2"));

        assertThat(rice.pullDomainEvents()).containsExactly(new PantryItemReachedMinimum(rice.getPantryItemId()));
    }

    @Test
    void loweringTheMinimumCanAlsoEndTheShortage() {
        rice.changeMinimumAmount(new BigDecimal("2"));
        rice.pullDomainEvents();

        rice.changeMinimumAmount(BigDecimal.ZERO);

        assertThat(rice.pullDomainEvents()).containsExactly(new PantryItemReachedMinimum(rice.getPantryItemId()));
    }

    @Test
    void changesThatDoNotCrossTheMinimumRaiseNoEvent() {
        rice.changeQuantity(new BigDecimal("5"));
        rice.changeMinimumAmount(new BigDecimal("3"));
        rice.changeQuantity(new BigDecimal("4"));

        assertThat(rice.pullDomainEvents()).isEmpty();
    }

    @Test
    void restoringAnItemBelowMinimumRaisesNoEvent() {
        PantryItem restored = new PantryItem(PantryItemId.newId(), new ItemName("Mehl"),
                new Quantity(BigDecimal.ONE, Unit.KILOGRAM), null, StorageLocationId.DEFAULT, new BigDecimal("3"));

        assertThat(restored.isBelowMinimum()).isTrue();
        assertThat(restored.pullDomainEvents()).isEmpty();
    }

    @Test
    void missingQuantityIsTheGapToTheMinimum() {
        rice.changeMinimumAmount(new BigDecimal("2.5"));

        assertThat(rice.getMissingQuantity()).isEqualTo(new Quantity(new BigDecimal("1.5"), Unit.KILOGRAM));

        rice.changeQuantity(BigDecimal.TEN);
        assertThat(rice.getMissingQuantity()).isEqualTo(new Quantity(BigDecimal.ZERO, Unit.KILOGRAM));
    }

    @Test
    void restockAddsThePurchasedAmount() {
        rice.restock(new BigDecimal("1.5"));

        assertThat(rice.getQuantity().amount()).isEqualByComparingTo("2.5");
    }

    @Test
    void restockRejectsAmountsThatAreNotPositive() {
        assertThatIllegalArgumentException().isThrownBy(() -> rice.restock(BigDecimal.ZERO));
        assertThatIllegalArgumentException().isThrownBy(() -> rice.restock(new BigDecimal("-1")));
    }

    @Test
    void partialRestockStaysBelowMinimumWithoutEvent() {
        rice.changeMinimumAmount(new BigDecimal("4"));
        rice.pullDomainEvents();

        rice.restock(BigDecimal.ONE);

        assertThat(rice.isBelowMinimum()).isTrue();
        assertThat(rice.getMissingQuantity().amount()).isEqualByComparingTo("2");
        assertThat(rice.pullDomainEvents()).isEmpty();
    }

    @Test
    void restockToTheMinimumRaisesReachedMinimum() {
        rice.changeMinimumAmount(new BigDecimal("4"));
        rice.pullDomainEvents();

        rice.restock(new BigDecimal("3"));

        assertThat(rice.pullDomainEvents()).containsExactly(new PantryItemReachedMinimum(rice.getPantryItemId()));
    }
}
