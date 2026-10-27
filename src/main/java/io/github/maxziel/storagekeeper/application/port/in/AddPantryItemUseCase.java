package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.PantryItem;

/** Adds a new item to the pantry. */
public interface AddPantryItemUseCase {

    /** Creates the item with a new id, stores it and return it */
    PantryItem add(AddPantryItemCommand command);
}
