package io.github.maxziel.storagekeeper.application.port.out;

import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntry;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntryId;

import java.util.List;

/** Storage for hand-written shopping list entries, as needed by the application. */
public interface ManualShoppingListRepository {

    ManualShoppingListEntry save(ManualShoppingListEntry entry);

    /** Returns all entries, oldest first. */
    List<ManualShoppingListEntry> findAll();

    /** @return {@code false} if there was no entry with this id */
    boolean deleteById(ManualShoppingListEntryId manualShoppingListEntryId);
}
