package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.port.in.ShoppingList;

import java.util.List;

/**
 * JSON representation of the whole shopping list.
 *
 * @param stockPurchases "Vorratseinkäufe": items below their minimum stock
 * @param manualEntries  "Einkaufsliste": entries written by hand
 */
public record ShoppingListResponse(
        List<ShoppingListEntryResponse> stockPurchases,
        List<ManualShoppingListEntryResponse> manualEntries
) {
    static ShoppingListResponse from(ShoppingList shoppingList) {
        return new ShoppingListResponse(
                shoppingList.stockPurchases().stream().map(ShoppingListEntryResponse::from).toList(),
                shoppingList.manualEntries().stream().map(ManualShoppingListEntryResponse::from).toList());
    }
}
