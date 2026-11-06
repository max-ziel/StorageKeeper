package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntry;

import java.util.List;

/**
 * The whole shopping list in its two sections, each sorted oldest first.
 *
 * @param stockPurchases items below their minimum stock ("Vorratseinkäufe"), created automatically
 * @param manualEntries  entries written by hand ("Einkaufsliste")
 */
public record ShoppingList(List<ShoppingListItem> stockPurchases, List<ManualShoppingListEntry> manualEntries) {

    public ShoppingList {
        stockPurchases = List.copyOf(stockPurchases);
        manualEntries = List.copyOf(manualEntries);
    }
}
