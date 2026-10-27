package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.PantryItemId;

/** Removes an item from the pantry.*/
public interface RemovePantryItemUseCase {
    /** @throws io.github.maxziel.storagekeeper.application.PantryItemNotFoundException if no item has this id */
    void remove(PantryItemId id);
}
