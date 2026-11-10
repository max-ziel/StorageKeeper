package io.github.maxziel.storagekeeper.application.port.out;

import io.github.maxziel.storagekeeper.domain.HouseholdId;
import io.github.maxziel.storagekeeper.domain.LocationName;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import io.github.maxziel.storagekeeper.domain.StorageLocationId;

import java.util.List;
import java.util.Optional;

/**
 * Storage for storage locations, as needed by the application.
 */
public interface StorageLocationRepository {

    StorageLocation save(StorageLocation location);

    /**
     * Returns the household's locations sorted by name.
     */
    List<StorageLocation> findAll(HouseholdId householdId);

    /**
     * Finds a location of any household; the caller checks {@link StorageLocation#belongsTo}.
     */
    Optional<StorageLocation> findById(StorageLocationId storageLocationId);

    /**
     * The household's default location; every household has exactly one.
     */
    Optional<StorageLocation> findDefault(HouseholdId householdId);

    /**
     * Whether the household has a location with this name. Compares names ignoring case, so "Keller" and
     * "keller" count as the same. Other households may use the same name.
     */
    boolean existsByName(HouseholdId householdId, LocationName locationName);
}
