package io.github.maxziel.storagekeeper.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of a {@link StorageLocation}.
 *
 * @param value the underlying UUID
 */
public record StorageLocationId(UUID value) {

    /** Id of the base location that is created by the database migration and used when none is given. */
    public static final StorageLocationId DEFAULT =
            new StorageLocationId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

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
