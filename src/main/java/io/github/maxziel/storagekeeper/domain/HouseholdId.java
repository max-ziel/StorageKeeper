package io.github.maxziel.storagekeeper.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a {@link Household}.
 *
 * @param value the underlying UUID
 */
public record HouseholdId(UUID value) {

    public HouseholdId {
        Objects.requireNonNull(value, "id must not be null");
    }

    public static HouseholdId newId() {
        return new HouseholdId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
