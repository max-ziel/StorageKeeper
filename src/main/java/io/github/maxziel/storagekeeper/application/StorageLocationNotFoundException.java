package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.domain.StorageLocationId;

/**
 * Thrown when a use case refers to a storage location that does not exist.
 */
public class StorageLocationNotFoundException extends RuntimeException {
    public StorageLocationNotFoundException(StorageLocationId storageLocationId) {
        super("Storage location " + storageLocationId + " not found");
    }
}
