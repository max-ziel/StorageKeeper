package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.application.port.out.StorageLocationRepository;
import io.github.maxziel.storagekeeper.domain.HouseholdId;
import io.github.maxziel.storagekeeper.domain.LocationName;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import io.github.maxziel.storagekeeper.domain.StorageLocationId;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implements the {@link StorageLocationRepository} port with PostgreSQL via Spring Data JPA.
 */
@Repository
class JpaStorageLocationRepository implements StorageLocationRepository {

    private final SpringDataStorageLocationRepository jpaRepository;

    JpaStorageLocationRepository(SpringDataStorageLocationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StorageLocation save(StorageLocation storageLocation) {
        jpaRepository.save(StorageLocationJpaEntity.fromDomain(storageLocation));
        return storageLocation;
    }

    @Override
    public List<StorageLocation> findAll(HouseholdId householdId) {
        return jpaRepository.findByHouseholdId(householdId.value(), Sort.by("name"))
            .stream()
            .map(StorageLocationJpaEntity::toDomain)
            .toList();
    }

    @Override
    public Optional<StorageLocation> findById(StorageLocationId storageLocationId) {
        return jpaRepository.findById(storageLocationId.value()).map(StorageLocationJpaEntity::toDomain);
    }

    @Override
    public Optional<StorageLocation> findDefault(HouseholdId householdId) {
        return jpaRepository.findByHouseholdIdAndDefaultLocationTrue(householdId.value())
            .map(StorageLocationJpaEntity::toDomain);
    }

    @Override
    public boolean existsByName(HouseholdId householdId, LocationName locationName) {
        return jpaRepository.existsByHouseholdIdAndNameIgnoreCase(householdId.value(), locationName.value());
    }
}
