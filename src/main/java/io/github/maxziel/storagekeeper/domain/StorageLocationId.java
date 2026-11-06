package io.github.maxziel.storagekeeper.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a {@link StorageLocation}.
 *
 * @param value the underlying UUID
 */
public record StorageLocationId(UUID value) {

    public StorageLocationId {
        Objects.requireNonNull(value, "id must not be null");
    }

    public static StorageLocationId newId() {
        return new StorageLocationId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
