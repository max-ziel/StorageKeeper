package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.port.in.AddStorageLocationUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ListStorageLocationsUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST endpoints for storage locations. */
@RestController
@RequestMapping("/api/storage-locations")
class StorageLocationController {

    private final AddStorageLocationUseCase addStorageLocation;
    private final ListStorageLocationsUseCase listStorageLocations;

    StorageLocationController(AddStorageLocationUseCase addStorageLocation, ListStorageLocationsUseCase listStorageLocations) {
        this.addStorageLocation = addStorageLocation;
        this.listStorageLocations = listStorageLocations;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    StorageLocationResponse add(@Valid @RequestBody AddStorageLocationRequest request) {
        return StorageLocationResponse.from(addStorageLocation.add(request.toLocationName()));
    }

    @GetMapping
    List<StorageLocationResponse> listAll() {
        return listStorageLocations.listAll()
                .stream()
                .map(StorageLocationResponse::from)
                .toList();
    }
}
