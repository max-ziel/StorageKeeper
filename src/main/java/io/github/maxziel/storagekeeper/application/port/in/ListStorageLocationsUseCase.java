package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.StorageLocation;

import java.util.List;

/** Lists all storage locations. */
public interface ListStorageLocationsUseCase {
    /** Returns all locations sorted by name, including the default location. */
    List<StorageLocation> listAll();
}
