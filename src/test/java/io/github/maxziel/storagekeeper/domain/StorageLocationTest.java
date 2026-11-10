package io.github.maxziel.storagekeeper.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StorageLocationTest {

    private static final HouseholdId HOUSEHOLD = HouseholdId.newId();

    @Test
    void createdLocationIsNotTheDefault() {
        StorageLocation cellar = StorageLocation.create(HOUSEHOLD, new LocationName("Keller"));

        assertThat(cellar.isDefault()).isFalse();
        assertThat(cellar.getHouseholdId()).isEqualTo(HOUSEHOLD);
    }

    @Test
    void createDefaultCreatesTheBaseLocationOfTheHousehold() {
        StorageLocation pantry = StorageLocation.createDefault(HOUSEHOLD);

        assertThat(pantry.isDefault()).isTrue();
        assertThat(pantry.getLocationName()).isEqualTo(new LocationName("Vorrat"));
        assertThat(pantry.belongsTo(HOUSEHOLD)).isTrue();
        assertThat(pantry.belongsTo(HouseholdId.newId())).isFalse();
    }

    @Test
    void eachHouseholdGetsItsOwnDefaultLocation() {
        StorageLocation first = StorageLocation.createDefault(HOUSEHOLD);
        StorageLocation second = StorageLocation.createDefault(HouseholdId.newId());

        assertThat(first.getStorageLocationId()).isNotEqualTo(second.getStorageLocationId());
    }

    @Test
    void equalityIsBasedOnIdOnly() {
        StorageLocation cellar = StorageLocation.create(HOUSEHOLD, new LocationName("Keller"));
        StorageLocation renamed = new StorageLocation(
            cellar.getStorageLocationId(), HOUSEHOLD, new LocationName("Gewölbekeller"), false);

        assertThat(renamed).isEqualTo(cellar).hasSameHashCodeAs(cellar);
    }
}
