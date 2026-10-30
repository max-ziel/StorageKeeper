package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.PantryItem;

/**
 * Result of {@link ListExpiringPantryItemsUseCase}: an item together with the days left until it expires,
 * counted from the day the query ran.
 *
 * @param daysUntilExpiry 0 = expires today, negative = already expired
 */
public record ExpiringPantryItem(PantryItem item, long daysUntilExpiry) {

    public boolean expired() {
        return daysUntilExpiry < 0;
    }
}
