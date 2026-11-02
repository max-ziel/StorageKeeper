package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.in.AddStorageLocationUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ListStorageLocationsUseCase;
import io.github.maxziel.storagekeeper.application.port.out.StorageLocationRepository;
import io.github.maxziel.storagekeeper.domain.LocationName;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Implements the storage location use cases.
 */
@Service
public class StorageLocationService implements AddStorageLocationUseCase, ListStorageLocationsUseCase {

    private final StorageLocationRepository repository;

    public StorageLocationService(StorageLocationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }
    @Override
    public StorageLocation add(LocationName name) {
        if (repository.existsByName(name)) {
            throw new StorageLocationAlreadyExistsException(name);
        }
        return repository.save(StorageLocation.create(name));
    }

    @Override
    public List<StorageLocation> listAll() {
        return repository.findAll();
    }
}
