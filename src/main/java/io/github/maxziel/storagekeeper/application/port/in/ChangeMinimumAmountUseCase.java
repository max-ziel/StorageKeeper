package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;

import java.math.BigDecimal;

/**
 * Changes the minimum stock of an existing item; the unit is the item's unit.
 */
public interface ChangeMinimumAmountUseCase {

    /**
     * @param newMinimumAmount 0 = no minimum
     * @throws io.github.maxziel.storagekeeper.application.PantryItemNotFoundException if no item has this id
     * @throws io.github.maxziel.storagekeeper.domain.DomainValidationException        if the amount is negative
     */
    PantryItem changeMinimumAmount(PantryItemId pantryItemId, BigDecimal newMinimumAmount);
}
