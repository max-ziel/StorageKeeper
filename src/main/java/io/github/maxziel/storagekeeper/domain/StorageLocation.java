package io.github.maxziel.storagekeeper.domain;

import java.util.Objects;

/**
 * A place where pantry items are kept, e.g. fridge or cellar. Entity and aggregate root of its own:
 * a {@link PantryItem} refers to it only by {@link StorageLocationId}.
 */
public class StorageLocation {

    private final StorageLocationId storageLocationId;
    private final LocationName locationName;

    /**
     * Restores a location with a known id, e.g., when loading it from storage.
     * Use {@link #create} for new locations.
     */
    public StorageLocation(StorageLocationId storageLocationId, LocationName locationName) {
        this.storageLocationId = Objects.requireNonNull(storageLocationId, "id must not be null");
        this.locationName = Objects.requireNonNull(locationName, "name must not be null");
    }

    /**
     * Creates a new location with a freshly generated id.
     */
    public static StorageLocation create(LocationName locationName) {
        return new StorageLocation(StorageLocationId.newId(), locationName);
    }

    public StorageLocationId getStorageLocationId() {
        return storageLocationId;
    }

    public LocationName getLocationName() {
        return locationName;
    }

    /** {@code true} for the base location that new items go to when no location is given. */
    public boolean isDefault() {
        return StorageLocationId.DEFAULT.equals(storageLocationId);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StorageLocation other && storageLocationId.equals(other.storageLocationId);
    }

    @Override
    public int hashCode() {
        return storageLocationId.hashCode();
    }

}
