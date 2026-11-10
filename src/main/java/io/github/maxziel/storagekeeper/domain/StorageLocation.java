package io.github.maxziel.storagekeeper.domain;

import java.util.Objects;

/**
 * A place where pantry items are kept, e.g. fridge or cellar. Entity and aggregate root of its own:
 * a {@link PantryItem} refers to it only by {@link StorageLocationId}. It belongs to exactly one
 * {@link Household}; each household has exactly one default location.
 */
public class StorageLocation {

    /**
     * Name of the default location every household gets when it is created.
     */
    public static final LocationName DEFAULT_NAME = new LocationName("Vorrat");

    private final StorageLocationId storageLocationId;
    private final HouseholdId householdId;
    private final LocationName locationName;
    private final boolean defaultLocation;

    /**
     * Restores a location with a known id, e.g., when loading it from storage.
     * Use {@link #create} or {@link #createDefault} for new locations.
     */
    public StorageLocation(StorageLocationId storageLocationId, HouseholdId householdId, LocationName locationName,
                           boolean defaultLocation) {
        this.storageLocationId = Objects.requireNonNull(storageLocationId, "id must not be null");
        this.householdId = Objects.requireNonNull(householdId, "household must not be null");
        this.locationName = Objects.requireNonNull(locationName, "name must not be null");
        this.defaultLocation = defaultLocation;
    }

    /**
     * Creates a new location with a freshly generated id.
     */
    public static StorageLocation create(HouseholdId householdId, LocationName locationName) {
        return new StorageLocation(StorageLocationId.newId(), householdId, locationName, false);
    }

    /**
     * Creates the default location "Vorrat" for a new household.
     */
    public static StorageLocation createDefault(HouseholdId householdId) {
        return new StorageLocation(StorageLocationId.newId(), householdId, DEFAULT_NAME, true);
    }

    public StorageLocationId getStorageLocationId() {
        return storageLocationId;
    }

    public HouseholdId getHouseholdId() {
        return householdId;
    }

    /**
     * {@code true} if the location is part of this household's pantry.
     */
    public boolean belongsTo(HouseholdId household) {
        return householdId.equals(household);
    }

    public LocationName getLocationName() {
        return locationName;
    }

    /**
     * {@code true} for the household's base location that new items go to when no location is given.
     */
    public boolean isDefault() {
        return defaultLocation;
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
