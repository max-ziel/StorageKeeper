package io.github.maxziel.storagekeeper.application.port.out;

import io.github.maxziel.storagekeeper.domain.PantryItemId;
import io.github.maxziel.storagekeeper.domain.ShoppingListEntry;
import io.github.maxziel.storagekeeper.domain.ShoppingListEntryId;

import java.util.List;
import java.util.Optional;

/** Storage for shopping list entries, as needed by the application. */
public interface ShoppingListRepository {

    ShoppingListEntry save(ShoppingListEntry entry);

    /** Returns all entries, oldest first. */
    List<ShoppingListEntry> findAll();

    boolean existsByPantryItemId(PantryItemId pantryItemId);

    /** Removes the entry for this item; does nothing if there is none. */
    void deleteByPantryItemId(PantryItemId pantryItemId);

    Optional<ShoppingListEntry> findById(ShoppingListEntryId shoppingListEntryId);
}
