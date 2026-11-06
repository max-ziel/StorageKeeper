package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntryId;
import io.github.maxziel.storagekeeper.domain.ShoppingListEntryId;

/** Thrown when a use case refers to a shopping list entry that does not exist (any more). */
public class ShoppingListEntryNotFoundException extends RuntimeException {

    public ShoppingListEntryNotFoundException(ShoppingListEntryId id) {
        super("Shopping list entry " + id + " not found");
    }

    public ShoppingListEntryNotFoundException(ManualShoppingListEntryId id) {
        super("Shopping list entry " + id + " not found");
    }
}
