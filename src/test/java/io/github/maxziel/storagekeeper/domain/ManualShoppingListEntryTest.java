package io.github.maxziel.storagekeeper.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class ManualShoppingListEntryTest {

    private static final HouseholdId HOUSEHOLD = HouseholdId.newId();
    private static final Instant NOW = Instant.parse("2027-02-26T10:00:00Z");

    @Test
    void quantityIsOptional() {
        ManualShoppingListEntry entry = ManualShoppingListEntry.create(HOUSEHOLD, new ItemName("Kerzen"), null, NOW);

        assertThat(entry.getQuantity()).isEmpty();
    }

    @Test
    void quantityMustBePositive() {
        assertThatIllegalArgumentException().isThrownBy(() -> ManualShoppingListEntry.create(HOUSEHOLD,
            new ItemName("Kerzen"), new Quantity(BigDecimal.ZERO, Unit.PIECE), NOW));
    }

    @Test
    void entriesWithTheSameIdAreEqual() {
        ManualShoppingListEntryId id = ManualShoppingListEntryId.newId();

        assertThat(new ManualShoppingListEntry(id, HOUSEHOLD, new ItemName("Kerzen"), null, NOW))
            .isEqualTo(new ManualShoppingListEntry(id, HOUSEHOLD, new ItemName("Ballons"), null, NOW.plusSeconds(60)));
    }

    @Test
    void timeOfAddingIsRequired() {
        assertThatNullPointerException()
            .isThrownBy(() -> ManualShoppingListEntry.create(HOUSEHOLD, new ItemName("Kerzen"), null, null))
            .withMessage("addedAt must not be null");
    }
}
