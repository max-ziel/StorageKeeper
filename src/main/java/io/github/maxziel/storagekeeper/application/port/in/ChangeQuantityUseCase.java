package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;

import java.math.BigDecimal;

/** Changes the amount of an existing item; the unit stays the same. */
public interface ChangeQuantityUseCase {

    /**
     * @throws io.github.maxziel.storagekeeper.application.PantryItemNotFoundException if no item has this id
     * @throws IllegalArgumentException if the new amount is negative
     */
    PantryItem changeQuantity(PantryItemId pantryItemId, BigDecimal newAmount);
}
