package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.TestcontainersConfiguration;
import io.github.maxziel.storagekeeper.adapter.out.persistence.TestHouseholds.StoredHousehold;
import io.github.maxziel.storagekeeper.domain.LocationName;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import io.github.maxziel.storagekeeper.domain.StorageLocationId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Tag("integration")
@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaStorageLocationRepository.class})
class JpaStorageLocationRepositoryTest {

    @Autowired
    private JpaStorageLocationRepository repository;

    @Autowired
    private EntityManager entityManager;

    private StoredHousehold household;
    private StoredHousehold otherHousehold;

    @BeforeEach
    void setUp() {
        household = TestHouseholds.create(entityManager);
        otherHousehold = TestHouseholds.create(entityManager);
    }

    @Test
    void findDefaultReturnsTheDefaultLocationOfTheHousehold() {
        assertThat(repository.findDefault(household.id())).hasValueSatisfying(location -> {
            assertThat(location.getStorageLocationId()).isEqualTo(household.defaultLocation());
            assertThat(location.isDefault()).isTrue();
            assertThat(location.getLocationName()).isEqualTo(new LocationName("Vorrat"));
        });
    }

    @Test
    void savedLocationsAreListedByNameAndOnlyForTheirHousehold() {
        repository.save(StorageLocation.create(household.id(), new LocationName("Kühlschrank")));
        repository.save(StorageLocation.create(household.id(), new LocationName("Keller")));
        repository.save(StorageLocation.create(otherHousehold.id(), new LocationName("Garage")));

        List<String> names = repository.findAll(household.id()).stream()
            .map(location -> location.getLocationName().value())
            .toList();

        assertThat(names).containsExactly("Keller", "Kühlschrank", "Vorrat");
    }

    @Test
    void findByIdLoadsAllFields() {
        StorageLocation cellar = repository.save(StorageLocation.create(household.id(), new LocationName("Keller")));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findById(cellar.getStorageLocationId())).hasValueSatisfying(loaded -> {
            assertThat(loaded.getHouseholdId()).isEqualTo(household.id());
            assertThat(loaded.getLocationName()).isEqualTo(new LocationName("Keller"));
            assertThat(loaded.isDefault()).isFalse();
        });
        assertThat(repository.findById(StorageLocationId.newId())).isEmpty();
    }

    @Test
    void existsByNameIgnoresCaseAndOnlyLooksAtTheHousehold() {
        repository.save(StorageLocation.create(household.id(), new LocationName("Keller")));

        assertThat(repository.existsByName(household.id(), new LocationName("keller"))).isTrue();
        assertThat(repository.existsByName(household.id(), new LocationName("Garage"))).isFalse();
        assertThat(repository.existsByName(otherHousehold.id(), new LocationName("Keller"))).isFalse();
    }

    @Test
    void twoHouseholdsMayUseTheSameName() {
        repository.save(StorageLocation.create(household.id(), new LocationName("Keller")));
        repository.save(StorageLocation.create(otherHousehold.id(), new LocationName("Keller")));
        entityManager.flush();

        assertThat(repository.existsByName(otherHousehold.id(), new LocationName("Keller"))).isTrue();
    }

    @Test
    void databaseAllowsOnlyOneDefaultLocationPerHousehold() {
        assertThatExceptionOfType(PersistenceException.class).isThrownBy(() -> {
            repository.save(StorageLocation.createDefault(household.id()));
            entityManager.flush();
        });
    }
}
