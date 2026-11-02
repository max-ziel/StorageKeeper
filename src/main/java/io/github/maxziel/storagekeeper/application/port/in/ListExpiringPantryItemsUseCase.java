package io.github.maxziel.storagekeeper.application.port.in;

import java.util.List;

/** Lists items that expire soon or have already expired. */
public interface ListExpiringPantryItemsUseCase {

    /**
     * Returns all items that expire within the next {@code days} days (counted from today),
     * including already expired ones, sorted by expiry date. Items without an expiry date are left out.
     *
     * @param days 0 = only items that expire today or earlier
     * @throws IllegalArgumentException if {@code days} is negative
     */
    List<ExpiringPantryItem> listExpiringWithin(int days);
}
