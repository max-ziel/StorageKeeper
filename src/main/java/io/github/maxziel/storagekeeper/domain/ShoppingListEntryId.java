package io.github.maxziel.storagekeeper.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a {@link ShoppingListEntry}
 * @param value
 */
public record ShoppingListEntryId(UUID value) {

    public ShoppingListEntryId {
        Objects.requireNonNull(value, "id must not be null");
    }

    public static ShoppingListEntryId newId() {
        return new ShoppingListEntryId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return  value.toString();
    }
}
