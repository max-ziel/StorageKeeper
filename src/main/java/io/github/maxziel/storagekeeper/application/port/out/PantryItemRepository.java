package io.github.maxziel.storagekeeper.application.port.out;

import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;

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
}
