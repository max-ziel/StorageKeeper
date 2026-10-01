package io.github.maxziel.storagekeeper.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record Quantity(BigDecimal amount, Unit unit) {

    public Quantity {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(unit, "unit must be null");
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
