package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.domain.LocationName;

/**
 * Thrown when a storage location with the same name already exists.
 */
public class StorageLocationAlreadyExistsException extends RuntimeException {
    public StorageLocationAlreadyExistsException(LocationName locationName) {
        super("Storage location " + locationName.value() + " already exists");
    }
}
