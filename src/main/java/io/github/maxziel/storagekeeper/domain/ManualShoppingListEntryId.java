package io.github.maxziel.storagekeeper.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a {@link ManualShoppingListEntry}.
 *
 * @param value the UUID, never {@code null}
 */
public record ManualShoppingListEntryId(UUID value) {

    public ManualShoppingListEntryId {
        Objects.requireNonNull(value, "id must not be null");
    }

    public static ManualShoppingListEntryId newId() {
        return new ManualShoppingListEntryId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
