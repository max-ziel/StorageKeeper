package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.port.in.ShoppingListItem;
import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.Unit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JSON representation of one shopping list entry.
 *
 * @param missingAmount how much has to be bought to reach the minimum stock, in {@code unit}
 */
public record ShoppingListEntryResponse(UUID id, UUID pantryItemId, String name, Unit unit, BigDecimal amount,
                                        BigDecimal minimumAmount, BigDecimal missingAmount, UUID storageLocationId,
                                        Instant addedAt) {

    static ShoppingListEntryResponse from(ShoppingListItem shoppingListItem) {
        PantryItem item = shoppingListItem.item();
        return new ShoppingListEntryResponse(
            shoppingListItem.entry().getShoppingListEntryId().value(),
            item.getPantryItemId().value(),
            item.getItemName().value(),
            item.getQuantity().unit(),
            item.getQuantity().amount(),
            item.getMinimumQuantity().amount(),
            item.getMissingQuantity().amount(),
            item.getStorageLocationId().value(),
            shoppingListItem.entry().getAddedAt());
    }
}
