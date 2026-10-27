package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.port.in.AddPantryItemCommand;
import io.github.maxziel.storagekeeper.domain.ItemName;
import io.github.maxziel.storagekeeper.domain.Quantity;
import io.github.maxziel.storagekeeper.domain.StorageLocationId;
import io.github.maxziel.storagekeeper.domain.Unit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JSON body for {@code POST /api/pantry-items}.
 *
 * @param storageLocationId optional; without it the item goes to the default location
 */
public record AddPantryItemRequest(
        @NotBlank @Size(max = ItemName.MAX_LENGTH) String name,
        @NotNull @PositiveOrZero BigDecimal amount,
        @NotNull Unit unit,
        LocalDate expiryDate,
        UUID storageLocationId) {

    AddPantryItemCommand toCommand() {
        return new AddPantryItemCommand(
                new ItemName(name),
                new Quantity(amount, unit),
                expiryDate,
                storageLocationId == null ? null : new StorageLocationId(storageLocationId));
    }
}
