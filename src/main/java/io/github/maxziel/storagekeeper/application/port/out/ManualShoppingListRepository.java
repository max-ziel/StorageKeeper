package io.github.maxziel.storagekeeper.application.port.out;

import io.github.maxziel.storagekeeper.domain.HouseholdId;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntry;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntryId;

import java.util.List;
import java.util.Optional;

/** Storage for hand-written shopping list entries, as needed by the application. */
public interface ManualShoppingListRepository {

    ManualShoppingListEntry save(ManualShoppingListEntry entry);

    /** Returns the household's entries, oldest first. */
    List<ManualShoppingListEntry> findAll(HouseholdId householdId);

    /** Finds an entry of any household; the caller checks {@link ManualShoppingListEntry#belongsTo}. */
    Optional<ManualShoppingListEntry> findById(ManualShoppingListEntryId manualShoppingListEntryId);

    /** @return {@code false} if there was no entry with this id */
    boolean deleteById(ManualShoppingListEntryId manualShoppingListEntryId);
}
