package io.github.maxziel.storagekeeper.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.maxziel.storagekeeper.application.port.in.ExpiringPantryItem;

/**
 * JSON representation of an item in the "expiring soon" list: all fields of {@link PantryItemResponse} on the same
 * level (not nested), plus how long the item still keeps. A new field in {@code PantryItemResponse} appears here too.
 *
 * @param daysUntilExpiry 0 = expires today, negative = already expired
 */
public record ExpiringPantryItemResponse(
    @JsonUnwrapped PantryItemResponse item,
    long daysUntilExpiry,
    boolean expired
) {

    static ExpiringPantryItemResponse from(ExpiringPantryItem expiring) {
        return new ExpiringPantryItemResponse(
            PantryItemResponse.from(expiring.item()),
            expiring.daysUntilExpiry(),
            expiring.expired()
        );
    }
}
