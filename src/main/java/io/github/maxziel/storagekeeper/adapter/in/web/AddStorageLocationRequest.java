package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.domain.LocationName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body for {@code POST /api/storage-locations}.
 */
public record AddStorageLocationRequest(@NotBlank @Size(max = LocationName.MAX_LENGTH) String name) {
    LocationName toLocationName() {
        return new LocationName(name);
    }
}
