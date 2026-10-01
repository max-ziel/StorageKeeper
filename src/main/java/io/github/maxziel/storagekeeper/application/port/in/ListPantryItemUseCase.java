package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.PantryItem;

import java.util.List;

/** Lists the items in the pantry. */
public interface ListPantryItemUseCase {
/** Returns all items; the order is not defined. */
    List<PantryItem> listAll();
}
