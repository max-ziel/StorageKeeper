package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.domain.Household;
import io.github.maxziel.storagekeeper.domain.HouseholdId;
import io.github.maxziel.storagekeeper.domain.HouseholdMember;
import io.github.maxziel.storagekeeper.domain.HouseholdName;
import io.github.maxziel.storagekeeper.domain.InviteCode;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import io.github.maxziel.storagekeeper.domain.StorageLocationId;
import io.github.maxziel.storagekeeper.domain.UserId;
import jakarta.persistence.EntityManager;

import java.util.UUID;

/**
 * Test data for the repository tests: every item, location and entry needs a household in the database
 * (foreign keys), and items need a location of that household.
 */
final class TestHouseholds {

    private TestHouseholds() {
    }

    /** A stored household with one member and its default location. */
    record StoredHousehold(Household household, StorageLocationId defaultLocation) {
        HouseholdId id() {
            return household.getHouseholdId();
        }
    }

    static StoredHousehold create(EntityManager entityManager) {
        Household household = Household.found(new HouseholdName("Testhaushalt"),
                new HouseholdMember(new UserId(UUID.randomUUID().toString()), "test"), InviteCode.generate());
        StorageLocation defaultLocation = StorageLocation.createDefault(household.getHouseholdId());
        entityManager.persist(HouseholdJpaEntity.fromDomain(household));
        entityManager.persist(StorageLocationJpaEntity.fromDomain(defaultLocation));
        entityManager.flush();
        return new StoredHousehold(household, defaultLocation.getStorageLocationId());
    }
}
