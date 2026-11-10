package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.ItemName;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntry;
import io.github.maxziel.storagekeeper.domain.Quantity;

/**
 * Writes something on the shopping list by hand.
 */
public interface AddManualShoppingListEntryUseCase {

    /**
     * @param quantity optional, {@code null} if no amount was given
     * @throws io.github.maxziel.storagekeeper.domain.DomainValidationException if the quantity is zero
     */
    ManualShoppingListEntry add(ItemName name, Quantity quantity);
}
