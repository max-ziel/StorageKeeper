package io.github.maxziel.storagekeeper.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Amount of a {@link PantryItem} together with its {@link Unit}, e.g. 1.5 KILOGRAM.
 *
 * <p>Invariants: amount and unit are set, amount is not negative. Zero is allowed
 * and means "used up".
 */
public record Quantity(BigDecimal amount, Unit unit) {

    public Quantity {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
        if (amount.signum() < 0) {
            throw  new IllegalArgumentException("amount must not be negative");
        }
    }

    /**
     * Return a new quantity with the give amount and the unit
     */
    public Quantity withAmount(BigDecimal newAmount) {
        return  new Quantity(newAmount, unit);
    }
}
