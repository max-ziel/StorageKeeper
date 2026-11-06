package io.github.maxziel.storagekeeper.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * JSON body for {@code POST /api/shopping-list/stock-purchases/{id}/check-off}.
 *
 * @param amount the amount bought, in the item's unit
 */
public record CheckOffStockPurchaseRequest(@NotNull @Positive BigDecimal amount) {
}
