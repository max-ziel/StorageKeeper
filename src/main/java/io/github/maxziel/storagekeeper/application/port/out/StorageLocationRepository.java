package io.github.maxziel.storagekeeper.application.port.out;

import io.github.maxziel.storagekeeper.domain.LocationName;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import io.github.maxziel.storagekeeper.domain.StorageLocationId;

import java.util.List;

/** Storage for storage locations, as needed by the application. */
public interface StorageLocationRepository {

    StorageLocation save(StorageLocation location);

    /** Returns all locations sorted by name. */
    List<StorageLocation> findAll();

    boolean existsById(StorageLocationId storageLocationId);

    /** Compares names ignoring case, so "Keller" and "keller" count as the same. */
    boolean existsByName(LocationName locationName);
}
