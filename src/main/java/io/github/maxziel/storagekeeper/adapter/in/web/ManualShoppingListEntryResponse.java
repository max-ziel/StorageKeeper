package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntry;
import io.github.maxziel.storagekeeper.domain.Quantity;
import io.github.maxziel.storagekeeper.domain.Unit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JSON representation of a handwritten shopping list entry.
 *
 * @param amount {@code null} together with {@code unit} if no amount was given
 */
public record ManualShoppingListEntryResponse(UUID id, String name, BigDecimal amount, Unit unit, Instant addedAt) {

    static ManualShoppingListEntryResponse from(ManualShoppingListEntry entry) {
        return new ManualShoppingListEntryResponse(
                entry.getManualShoppingListEntryId().value(),
                entry.getName().value(),
                entry.getQuantity().map(Quantity::amount).orElse(null),
                entry.getQuantity().map(Quantity::unit).orElse(null),
                entry.getAddedAt());
    }
}
