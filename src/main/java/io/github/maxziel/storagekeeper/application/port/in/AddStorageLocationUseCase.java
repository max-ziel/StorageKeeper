package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.LocationName;
import io.github.maxziel.storagekeeper.domain.StorageLocation;

/** Adds a new storage location. */
public interface AddStorageLocationUseCase {

    /**
     * @throws io.github.maxziel.storagekeeper.application.StorageLocationAlreadyExistsException
     *         if a location with this name (ignoring case) exists
     */
    StorageLocation add(LocationName name);
}
