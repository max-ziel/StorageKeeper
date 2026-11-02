package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.TestcontainersConfiguration;
import io.github.maxziel.storagekeeper.domain.LocationName;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import io.github.maxziel.storagekeeper.domain.StorageLocationId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaStorageLocationRepository.class})
class JpaStorageLocationRepositoryTest {

    @Autowired
    private JpaStorageLocationRepository repository;

    @Test
    void migrationCreatesDefaultLocation() {
        assertThat(repository.existsById(StorageLocationId.DEFAULT)).isTrue();

        assertThat(repository.findAll()).singleElement().satisfies(location -> {
           assertThat(location.isDefault()).isTrue();
           assertThat(location.getLocationName()).isEqualTo(new LocationName("Vorrat"));
        });
    }

    @Test
    void savedLocationsAreListedByName() {
        repository.save(StorageLocation.create(new LocationName("Kühlschrank")));
        repository.save(StorageLocation.create(new LocationName("Keller")));

        List<String> names = repository.findAll().stream()
                .map(location -> location.getLocationName().value())
                .toList();

        assertThat(names).containsExactly("Keller", "Kühlschrank", "Vorrat");
    }

    @Test
    void existsByNameIgnoresCase() {
        repository.save(StorageLocation.create(new LocationName("Keller")));

        assertThat(repository.existsByName(new LocationName("keller"))).isTrue();
        assertThat(repository.existsByName(new LocationName("Garage"))).isFalse();
    }
}
