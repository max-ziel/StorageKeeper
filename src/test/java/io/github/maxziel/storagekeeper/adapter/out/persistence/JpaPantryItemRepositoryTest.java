package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.TestcontainersConfiguration;
import io.github.maxziel.storagekeeper.adapter.out.persistence.TestHouseholds.StoredHousehold;
import io.github.maxziel.storagekeeper.application.port.PageQuery;
import io.github.maxziel.storagekeeper.application.port.ResultPage;
import io.github.maxziel.storagekeeper.application.port.in.PantryItemFilter;
import io.github.maxziel.storagekeeper.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Tag("integration")
@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaPantryItemRepository.class, JpaStorageLocationRepository.class})
class JpaPantryItemRepositoryTest {

    @Autowired
    private JpaPantryItemRepository repository;

    @Autowired
    private JpaStorageLocationRepository storageLocationRepository;

    @Autowired
    private EntityManager entityManager;

    private StoredHousehold household;
    /** Default location of {@link #household}. */
    private StorageLocationId pantry;
    private PantryItem rice;

    @BeforeEach
    void setUp() {
        household = TestHouseholds.create(entityManager);
        pantry = household.defaultLocation();
        rice = PantryItem.create(
                household.id(),
                new ItemName("Reis"),
                new Quantity(new BigDecimal("1.5"), Unit.KILOGRAM),
                LocalDate.of(2027, 3, 1),
                pantry
        );
    }

    @Test
    void savedItemCanBeLoadedWithAllFields() {
        repository.save(rice);

        PantryItem loaded = repository.findById(rice.getPantryItemId()).orElseThrow();

        assertThat(loaded).isEqualTo(rice);
        assertThat(loaded.getItemName()).isEqualTo(rice.getItemName());
        assertThat(loaded.getQuantity()).isEqualTo(rice.getQuantity());
        assertThat(loaded.getExpiryDate()).isEqualTo(rice.getExpiryDate());
        assertThat(loaded.getStorageLocationId()).isEqualTo(pantry);
        assertThat(loaded.getHouseholdId()).isEqualTo(household.id());
    }

    @Test
    void itemCanBeStoredInUserDefinedLocation() {
        StorageLocation cellar = storageLocationRepository.save(
                StorageLocation.create(household.id(), new LocationName("Keller")));
        PantryItem wine = PantryItem.create(household.id(), new ItemName("Wein"),
                new Quantity(BigDecimal.ONE, Unit.PIECE), null, cellar.getStorageLocationId());

        repository.save(wine);

        assertThat(repository.findById(wine.getPantryItemId()).orElseThrow().getStorageLocationId())
                .isEqualTo(cellar.getStorageLocationId());
    }

    @Test
    void itemInUnknownLocationIsRejectedByDatabase() {
        PantryItem lost = PantryItem.create(household.id(), new ItemName("Mehl"),
                new Quantity(BigDecimal.ONE, Unit.KILOGRAM), null, StorageLocationId.newId());

        assertThatExceptionOfType(PersistenceException.class)
                .isThrownBy(() -> {
                    repository.save(lost);
                    entityManager.flush();
                });
    }

    @Test
    void itemInLocationOfAnotherHouseholdIsRejectedByDatabase() {
        StoredHousehold otherHousehold = TestHouseholds.create(entityManager);
        PantryItem misplaced = PantryItem.create(household.id(), new ItemName("Mehl"),
                new Quantity(BigDecimal.ONE, Unit.KILOGRAM), null, otherHousehold.defaultLocation());

        assertThatExceptionOfType(PersistenceException.class)
                .isThrownBy(() -> {
                    repository.save(misplaced);
                    entityManager.flush();
                });
    }

    @Test
    void itemWithoutExpiryDateCanBeStored() {
        PantryItem salt = PantryItem.create(household.id(), new ItemName("Salz"),
                new Quantity(BigDecimal.ONE, Unit.PACKAGE), null, pantry);
        repository.save(salt);

        assertThat(repository.findById(salt.getPantryItemId()).orElseThrow().getExpiryDate()).isEmpty();
    }

    @Test
    void saveUpdatesExistingItem() {
        repository.save(rice);
        rice.changeQuantity(new BigDecimal("0.5"));
        repository.save(rice);

        assertThat(repository.findById(rice.getPantryItemId()).orElseThrow().getQuantity().amount())
                .isEqualByComparingTo("0.5");
        assertThat(repository.search(household.id(), PantryItemFilter.none(), new PageQuery(0, 10)).totalElements())
                .isEqualTo(1);
    }

    @Test
    void deleteByIdReportsWhetherItemExisted() {
        repository.save(rice);

        assertThat(repository.deleteById(rice.getPantryItemId())).isTrue();
        assertThat(repository.deleteById(rice.getPantryItemId())).isFalse();
        assertThat(repository.findById(rice.getPantryItemId())).isEmpty();
    }

    @Test
    void findByUnknownIdReturnsEmpty() {
        assertThat(repository.findById(PantryItemId.newId())).isEmpty();
    }

    @Test
    void findWithExpiryDateUpToReturnsItemsUpToTheDateSortedByExpiry() {
        PantryItem milk = PantryItem.create(household.id(), new ItemName("Milch"),
                new Quantity(BigDecimal.ONE, Unit.LITER), LocalDate.of(2027, 2, 24), pantry);
        PantryItem flour = PantryItem.create(household.id(), new ItemName("Mehl"),
                new Quantity(BigDecimal.ONE, Unit.KILOGRAM), LocalDate.of(2027, 6, 1), pantry);
        PantryItem salt = PantryItem.create(household.id(), new ItemName("Salz"),
                new Quantity(BigDecimal.ONE, Unit.PACKAGE), null, pantry);
        StoredHousehold otherHousehold = TestHouseholds.create(entityManager);
        PantryItem foreignMilk = PantryItem.create(otherHousehold.id(), new ItemName("Milch"),
                new Quantity(BigDecimal.ONE, Unit.LITER), LocalDate.of(2027, 2, 20), otherHousehold.defaultLocation());
        repository.save(rice);
        repository.save(milk);
        repository.save(flour);
        repository.save(salt);
        repository.save(foreignMilk);

        assertThat(repository.findWithExpiryDateUpTo(household.id(), LocalDate.of(2027, 3, 1)))
                .containsExactly(milk, rice);
    }

    @Test
    void minimumAmountIsStored() {
        rice.changeMinimumAmount(new BigDecimal("2.5"));
        repository.save(rice);

        PantryItem loaded = repository.findById(rice.getPantryItemId()).orElseThrow();

        assertThat(loaded.getMinimumQuantity()).isEqualTo(new Quantity(new BigDecimal("2.5"), Unit.KILOGRAM));
        assertThat(loaded.isBelowMinimum()).isTrue();
    }

    @Test
    void searchFiltersByLocationAndMinimumAndSortsByName() {
        StorageLocation cellar = storageLocationRepository.save(
                StorageLocation.create(household.id(), new LocationName("Keller")));
        PantryItem wine = PantryItem.create(household.id(), new ItemName("Wein"),
                new Quantity(BigDecimal.ONE, Unit.PIECE), null, cellar.getStorageLocationId());
        wine.changeMinimumAmount(new BigDecimal("6"));
        PantryItem juice = PantryItem.create(household.id(), new ItemName("Apfelsaft"),
                new Quantity(BigDecimal.TEN, Unit.LITER), null, cellar.getStorageLocationId());
        juice.changeMinimumAmount(new BigDecimal("2"));
        rice.changeMinimumAmount(new BigDecimal("5"));
        repository.save(wine);
        repository.save(juice);
        repository.save(rice);

        assertThat(names(repository.search(household.id(), PantryItemFilter.none(), new PageQuery(0, 10))))
                .containsExactly("Apfelsaft", "Reis", "Wein");
        assertThat(names(repository.search(household.id(),
                new PantryItemFilter(cellar.getStorageLocationId(), false), new PageQuery(0, 10))))
                .containsExactly("Apfelsaft", "Wein");
        assertThat(names(repository.search(household.id(), new PantryItemFilter(null, true), new PageQuery(0, 10))))
                .containsExactly("Reis", "Wein");
        assertThat(names(repository.search(household.id(),
                new PantryItemFilter(cellar.getStorageLocationId(), true), new PageQuery(0, 10))))
                .containsExactly("Wein");
    }

    @Test
    void searchOnlyReturnsItemsOfTheHousehold() {
        StoredHousehold otherHousehold = TestHouseholds.create(entityManager);
        repository.save(rice);
        repository.save(PantryItem.create(otherHousehold.id(), new ItemName("Fremder Reis"),
                new Quantity(BigDecimal.ONE, Unit.KILOGRAM), null, otherHousehold.defaultLocation()));

        ResultPage<PantryItem> page = repository.search(household.id(), PantryItemFilter.none(), new PageQuery(0, 10));

        assertThat(names(page)).containsExactly("Reis");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void searchReturnsRequestedPage() {
        for (String name : List.of("A", "B", "C", "D", "E")) {
            repository.save(PantryItem.create(household.id(), new ItemName(name),
                    new Quantity(BigDecimal.ONE, Unit.PIECE), null, pantry));
        }

        ResultPage<PantryItem> secondPage = repository.search(household.id(), PantryItemFilter.none(), new PageQuery(1, 2));

        assertThat(names(secondPage)).containsExactly("C", "D");
        assertThat(secondPage.totalElements()).isEqualTo(5);
        assertThat(secondPage.totalPages()).isEqualTo(3);
    }

    private static List<String> names(ResultPage<PantryItem> page) {
        return page.content().stream().map(item -> item.getItemName().value()).toList();
    }
}
