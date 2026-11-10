package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.ShoppingListEntryId;

import java.math.BigDecimal;

/**
 * Checks off a stock purchase ("Vorratseinkauf"): the bought amount is added to the item.
 * The entry disappears only once the item has reached its minimum again; after partial purchase
 * it stays with the remaining missing amount.
 */
public interface CheckOffStockPurchaseUseCase {

    /**
     * @return the item with its new stock
     * @throws io.github.maxziel.storagekeeper.application.ShoppingListEntryNotFoundException if there is no such entry
     * @throws io.github.maxziel.storagekeeper.domain.DomainValidationException if the amount is not positive
     */
    PantryItem checkOff(ShoppingListEntryId shoppingListEntryId, BigDecimal purchasedAmount);
}
