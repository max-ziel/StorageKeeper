package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.domain.HouseholdId;
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

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(nullable = false, length = LocationName.MAX_LENGTH)
    private String name;

    @Column(name = "default_location", nullable = false)
    private boolean defaultLocation;

    /** Required by JPA. */
    protected StorageLocationJpaEntity() {
    }

    static StorageLocationJpaEntity fromDomain(StorageLocation location) {
        StorageLocationJpaEntity entity = new StorageLocationJpaEntity();
        entity.id = location.getStorageLocationId().value();
        entity.householdId = location.getHouseholdId().value();
        entity.name = location.getLocationName().value();
        entity.defaultLocation = location.isDefault();
        return entity;
    }

    StorageLocation toDomain() {
        return new StorageLocation(
                new StorageLocationId(id), new HouseholdId(householdId), new LocationName(name), defaultLocation);
    }
}
