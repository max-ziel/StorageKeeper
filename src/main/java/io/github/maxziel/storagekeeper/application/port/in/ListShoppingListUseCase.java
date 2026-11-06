package io.github.maxziel.storagekeeper.application.port.in;

/**
 * Shows the shopping list
 */
public interface ListShoppingListUseCase {

    /** Returns both sections of the list, each oldest first. */
    ShoppingList getShoppingList();
}
