package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.port.in.AddStorageLocationUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ListStorageLocationsUseCase;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST endpoints for storage locations.
 */
@RestController
@Tag(name = "Lagerorte", description = "Orte, an denen Vorräte liegen")
@RequestMapping("/api/storage-locations")
class StorageLocationController {

    private final AddStorageLocationUseCase addStorageLocation;
    private final ListStorageLocationsUseCase listStorageLocations;

    StorageLocationController(AddStorageLocationUseCase addStorageLocation, ListStorageLocationsUseCase listStorageLocations) {
        this.addStorageLocation = addStorageLocation;
        this.listStorageLocations = listStorageLocations;
    }

    @PostMapping
    ResponseEntity<StorageLocationResponse> add(@Valid @RequestBody AddStorageLocationRequest request) {
        StorageLocation location = addStorageLocation.add(request.toLocationName());
        URI uri = URI.create("/api/storage-locations/" + location.getStorageLocationId());
        return ResponseEntity.created(uri).body(StorageLocationResponse.from(location));
    }

    @GetMapping
    List<StorageLocationResponse> listAll() {
        return listStorageLocations.listAll()
            .stream()
            .map(StorageLocationResponse::from)
            .toList();
    }
}
