package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.ShoppingListEntry;

/**
 * One line of the shopping list: the entry together with the item it refers to.
 */
public record ShoppingListItem(ShoppingListEntry entry, PantryItem item) {
}
