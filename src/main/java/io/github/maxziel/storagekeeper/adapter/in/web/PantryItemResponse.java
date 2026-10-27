package io.github.maxziel.storagekeeper.adapter.in.web;


import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** JSON representation of a pantry item. */
public record PantryItemResponse(UUID id, String name, BigDecimal amount, Unit unit, LocalDate expiryDate) {
    static PantryItemResponse from(PantryItem pantryItem) {
        return  new PantryItemResponse(
          pantryItem.getPantryItemId().value(),
          pantryItem.getItemName().value(),
          pantryItem.getQuantity().amount(),
          pantryItem.getQuantity().unit(),
          pantryItem.getExpiryDate().orElse(null)
        );
    }
}
