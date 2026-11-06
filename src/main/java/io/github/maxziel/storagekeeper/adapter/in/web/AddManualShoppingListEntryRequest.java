package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.domain.ItemName;
import io.github.maxziel.storagekeeper.domain.Quantity;
import io.github.maxziel.storagekeeper.domain.Unit;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * JSON body for {@code POST /api/shopping-list/stock-purchases/{id}/check-off}.
 *
 * @param amount the amount bought, in the item's unit
 */
public record AddManualShoppingListEntryRequest(
        @NotBlank @Size(max = ItemName.MAX_LENGTH) String name,
        @Positive BigDecimal amount,
        Unit unit
        ) {

    /**
     * Checked by Bean Validation like a field: either both or neither.
     */
    @AssertTrue(message = "amount and unit must be given together")
    public boolean isAmountAndUnitTogether() {
        return (amount == null) == (unit == null);
    }

    ItemName toName() {
        return new ItemName(name);
    }

    Quantity toQuantity() {
        return amount == null ? null : new Quantity(amount, unit);
    }
}
