package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.domain.StorageLocation;

import java.util.UUID;

/**
 * JSON representation of a storage location.
 *
 * @param defaultLocation {@code true} for the base location that items go to when none is given
 */
public record StorageLocationResponse(UUID id, String name, boolean defaultLocation) {

    static StorageLocationResponse from(StorageLocation location) {
        return new StorageLocationResponse(
                location.getStorageLocationId().value(),
                location.getLocationName().value(),
                location.isDefault()
        );
    }
}
