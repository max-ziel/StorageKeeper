package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;

import java.math.BigDecimal;

/**
 * Adds a purchased amount to the stock of an item.
 */
public interface RestockPantryItemUseCase {

    /**
     * @throws io.github.maxziel.storagekeeper.application.PantryItemNotFoundException if no item has this id
     * @throws IllegalArgumentException if the amount is not positive
     */
    PantryItem restock(PantryItemId pantryItemId, BigDecimal purchasedAmount);
}
