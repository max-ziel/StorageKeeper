package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.PantryItemId;

/**
 * Keeps the shopping list in line with the stock. Called when an item crosses its minimum stock,
 * not directly by users.
 */
public interface UpdateShoppingListUseCase {

    /**
     * Puts the item on the list; does nothing if it is already there.
     */
    void addItem(PantryItemId pantryItemId);

    /**
     * Takes the item off the list; does nothing if it is not there.
     */
    void removeItem(PantryItemId pantryItemId);
}
