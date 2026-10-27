package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.application.port.out.StorageLocationRepository;
import io.github.maxziel.storagekeeper.domain.LocationName;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import io.github.maxziel.storagekeeper.domain.StorageLocationId;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Implements the {@link StorageLocationRepository} port with PostgreSQL via Spring Data JPA. */
@Repository
class JpaStorageLocationRepository implements StorageLocationRepository {

    private final SpringDataStorageLocationRepository jpaRepository;

    JpaStorageLocationRepository(SpringDataStorageLocationRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public StorageLocation save(StorageLocation storageLocation) {
        jpaRepository.save(StorageLocationJpaEntity.fromDomain(storageLocation));
        return  storageLocation;
    }

    @Override
    public List<StorageLocation> findAll() {
        return jpaRepository.findAll(Sort.by("name"))
                .stream()
                .map(StorageLocationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(StorageLocationId storageLocationId) {
        return jpaRepository.existsById(storageLocationId.value());
    }

    @Override
    public boolean existsByName(LocationName locationName) {
        return jpaRepository.existsByNameIgnoreCase(locationName.value());
    }
}
