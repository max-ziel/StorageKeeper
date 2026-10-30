package io.github.maxziel.storagekeeper.application.port.out;

import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Storage for pantry items, as needed by the application. Implemented by a
 * driven adapter (in M1 in memory, from M2 on with JPA).
 */
public interface PantryItemRepository {

    /** Inserts or replaces the item and returns it */
    PantryItem save(PantryItem pantryItem);

    Optional<PantryItem> findById(PantryItemId pantryItemId);

    List<PantryItem> findAll();

    /**
     * @return {@code true} if an item was removed, {@code false} if the id was unknown
     */
    boolean deleteById(PantryItemId pantryItemId);

    /**
     * Returns all items whose expiry date is on or before {@code latest}, including already expired ones,
     * sorted by expiry date (earliest first). Items without an expiry date are not included.
     */
    List<PantryItem> findWithExpiryDateUpTo(LocalDate latest);
}
