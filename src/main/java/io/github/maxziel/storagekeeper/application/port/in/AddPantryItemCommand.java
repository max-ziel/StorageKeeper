package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.ItemName;
import io.github.maxziel.storagekeeper.domain.Quantity;
import io.github.maxziel.storagekeeper.domain.StorageLocationId;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input for {@link AddPantryItemUseCase}. Uses domain types, so the values are
 * already validated when the command is created.
 *
 * @param expiryDate        best-before date, or {@code null} if the product has none
 * @param storageLocationId where to keep the item, or {@code null} for the default location
 * @param minimumAmount     minimum stock in the item's unit, or {@code null} for no minimum
 */
public record AddPantryItemCommand(ItemName name, Quantity quantity, LocalDate expiryDate,
                                   StorageLocationId storageLocationId, BigDecimal minimumAmount) {
}
