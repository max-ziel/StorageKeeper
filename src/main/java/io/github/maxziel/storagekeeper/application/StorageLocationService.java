package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.in.AddStorageLocationUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ListStorageLocationsUseCase;
import io.github.maxziel.storagekeeper.application.port.out.StorageLocationRepository;
import io.github.maxziel.storagekeeper.domain.HouseholdId;
import io.github.maxziel.storagekeeper.domain.LocationName;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Implements the storage location use cases, always for the current user's household.
 */
@Service
public class StorageLocationService implements AddStorageLocationUseCase, ListStorageLocationsUseCase {

    private final StorageLocationRepository repository;
    private final CurrentHousehold currentHousehold;

    public StorageLocationService(StorageLocationRepository repository, CurrentHousehold currentHousehold) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.currentHousehold = Objects.requireNonNull(currentHousehold, "currentHousehold must not be null");
    }

    @Override
    public StorageLocation add(LocationName name) {
        HouseholdId household = currentHousehold.require();
        if (repository.existsByName(household, name)) {
            throw new StorageLocationAlreadyExistsException(name);
        }
        return repository.save(StorageLocation.create(household, name));
    }

    @Override
    public List<StorageLocation> listAll() {
        return repository.findAll(currentHousehold.require());
    }
}
