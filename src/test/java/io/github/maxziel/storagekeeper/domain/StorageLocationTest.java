package io.github.maxziel.storagekeeper.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StorageLocationTest {

    @Test
    void createdLocationIsNotTheDefault() {
        StorageLocation cellar = StorageLocation.create(new LocationName("Keller"));

        assertThat(cellar.getStorageLocationId()).isNotEqualTo(StorageLocationId.DEFAULT);

        assertThat(cellar.isDefault()).isFalse();
    }

    @Test
    void locationWithDefaultIdIsTheDefault() {
        StorageLocation pantry = new StorageLocation(StorageLocationId.DEFAULT, new LocationName("Vorrat"));

        assertThat(pantry.isDefault()).isTrue();
    }

    @Test
    void equalityIsBasedOnIdOnly() {
        StorageLocation cellar = StorageLocation.create(new LocationName("Keller"));
        StorageLocation renamed =
                new StorageLocation(cellar.getStorageLocationId(), new LocationName("Gewölbekeller"));

        assertThat(renamed).isEqualTo(cellar).hasSameHashCodeAs(cellar);
    }

}
