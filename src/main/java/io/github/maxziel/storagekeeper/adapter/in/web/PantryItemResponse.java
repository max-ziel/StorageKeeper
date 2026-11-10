package io.github.maxziel.storagekeeper.adapter.in.web;


import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JSON representation of a pantry item.
 *
 * @param minimumAmount minimum stock in {@code unit}; 0 = no minimum
 * @param belowMinimum  {@code true} if {@code amount} is below {@code minimumAmount}
 */
public record PantryItemResponse(UUID id, String name, BigDecimal amount, Unit unit, LocalDate expiryDate,
                                 UUID storageLocationId, BigDecimal minimumAmount, boolean belowMinimum) {

    static PantryItemResponse from(PantryItem pantryItem) {
        return new PantryItemResponse(
            pantryItem.getPantryItemId().value(),
            pantryItem.getItemName().value(),
            pantryItem.getQuantity().amount(),
            pantryItem.getQuantity().unit(),
            pantryItem.getExpiryDate().orElse(null),
            pantryItem.getStorageLocationId().value(),
            pantryItem.getMinimumQuantity().amount(),
            pantryItem.isBelowMinimum());
    }
}
