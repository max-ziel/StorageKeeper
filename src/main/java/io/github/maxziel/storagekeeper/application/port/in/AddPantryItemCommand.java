package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.ItemName;
import io.github.maxziel.storagekeeper.domain.Quantity;

import java.time.LocalDate;

/**
 * Input for {@link AddPantryItemUseCase}. Uses domain types, so the values are
 * already validated when the command is created.
 *
 * @param expiryDate best-before date, or {@code null} if the product has none
 */
public record AddPantryItemCommand(ItemName name, Quantity quantity, LocalDate expiryDate) { }
