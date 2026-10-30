package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.port.in.ExpiringPantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JSON representation of an item in the "expiring soon" list.
 *
 * @param daysUntilExpiry 0 = expires today, negative = already expired
 */
public record ExpiringPantryItemResponse(
        UUID id,
        String name,
        BigDecimal amount,
        Unit unit,
        LocalDate expiryDate,
        UUID storageLocationId,

        long daysUntilExpiry, boolean expired
) {

    static ExpiringPantryItemResponse from(ExpiringPantryItem expiring) {
        PantryItem item = expiring.item();
        return  new ExpiringPantryItemResponse(
                item.getPantryItemId().value(),
                item.getItemName().value(),
                item.getQuantity().amount(),
                item.getQuantity().unit(),
                item.getExpiryDate().orElseThrow(),
                item.getStorageLocationId().value(),
                expiring.daysUntilExpiry(),
                expiring.expired()
        );
    }
}
