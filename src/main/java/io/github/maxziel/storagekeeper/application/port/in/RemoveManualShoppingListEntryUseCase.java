package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntryId;

/**
 * Removes a handwritten entry, e.g. because it was bought.
 */
public interface RemoveManualShoppingListEntryUseCase {

    /**
     * @throws io.github.maxziel.storagekeeper.application.ShoppingListEntryNotFoundException if there is no such entry
     */
    void remove(ManualShoppingListEntryId manualShoppingListEntryId);
}
