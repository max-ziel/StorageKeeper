package io.github.maxziel.storagekeeper.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** JSON body for {@code PATCH /api/pantry-items/{id}/quantity}. */
public record ChangeQuantityRequest(@NotNull @PositiveOrZero BigDecimal amount) {
}
