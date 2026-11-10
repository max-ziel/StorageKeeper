package io.github.maxziel.storagekeeper.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * JSON body for {@code PATCH /api/pantry-items/{id}/minimum}.
 *
 * @param amount the new minimum stock in the item's unit; {@code 0} removes the minimum
 */
public record ChangeMinimumAmountRequest(@NotNull @PositiveOrZero BigDecimal amount) {
}
