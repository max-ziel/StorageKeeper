package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.domain.LocationName;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import io.github.maxziel.storagekeeper.domain.StorageLocationId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Database row of table {@code storage_location}. Flyway creates the table.
 */
@Entity
@Table(name = "storage_location")
class StorageLocationJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = LocationName.MAX_LENGTH)
    private String name;

    /** Required by JPA. */
    protected StorageLocationJpaEntity() {
    }

    static StorageLocationJpaEntity fromDomain(StorageLocation location) {
        StorageLocationJpaEntity entity = new StorageLocationJpaEntity();
        entity.id = location.getStorageLocationId().value();
        entity.name = location.getLocationName().value();
        return entity;
    }

    StorageLocation toDomain() {
        return new StorageLocation(new StorageLocationId(id), new LocationName(name));
    }
}
