package io.github.maxziel.storagekeeper.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a {@link PantryItem}. Value object wrapping a UUID so that ids of
 * different entities cannot be mixed up.
 *
 * @param value the underlying UUID
 */
public record PantryItemId(UUID value) {

    public PantryItemId {
        Objects.requireNonNull(value, "id must not be null");
    }

    public static PantryItemId newId() {
        return new PantryItemId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
