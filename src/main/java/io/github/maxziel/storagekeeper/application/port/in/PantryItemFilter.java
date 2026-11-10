package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.StorageLocationId;

/**
 * Filter for {@link ListPantryItemUseCase#search}. All criteria are combined with AND.
 *
 * @param storageLocationId only items in this location, or {@code null} for all locations
 * @param onlyBelowMinimum  {@code true} = only items whose amount is below their minimum stock
 */
public record PantryItemFilter(StorageLocationId storageLocationId, boolean onlyBelowMinimum) {

    /** No restriction: all items */
    public static PantryItemFilter none() {
        return new PantryItemFilter(null, false);
    }
}
