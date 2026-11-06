package io.github.maxziel.storagekeeper.application.port.in;

import java.util.List;

/**
 * Shows the shopping list
 */
public interface ListShoppingListUseCase {

    /** Returns all entries, oldest first. */
    List<ShoppingListItem> listEntries();
}
