package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.TestcontainersConfiguration;
import io.github.maxziel.storagekeeper.application.port.PageQuery;
import io.github.maxziel.storagekeeper.application.port.ResultPage;
import io.github.maxziel.storagekeeper.application.port.in.PantryItemFilter;
import io.github.maxziel.storagekeeper.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaPantryItemRepository.class, JpaStorageLocationRepository.class})
class JpaPantryItemRepositoryTest {

    @Autowired
    private JpaPantryItemRepository repository;

    @Autowired
    private JpaStorageLocationRepository storageLocationRepository;

    private final PantryItem rice = PantryItem.create(
            new ItemName("Reis"),
            new Quantity(new BigDecimal("1.5"), Unit.KILOGRAM),
            LocalDate.of(2027, 3, 1),
            StorageLocationId.DEFAULT
    );

    @Test
    void savedItemCanBeLoadedWithAllFields() {
        repository.save(rice);

        PantryItem loaded = repository.findById(rice.getPantryItemId()).orElseThrow();

        assertThat(loaded).isEqualTo(rice);
        assertThat(loaded.getItemName()).isEqualTo(rice.getItemName());
        assertThat(loaded.getQuantity()).isEqualTo(rice.getQuantity());
        assertThat(loaded.getExpiryDate()).isEqualTo(rice.getExpiryDate());
        assertThat(loaded.getStorageLocationId()).isEqualTo(StorageLocationId.DEFAULT);
    }

    @Test
    void itemCanBeStoredInUserDefinedLocation() {
        StorageLocation cellar = storageLocationRepository.save(StorageLocation.create(new LocationName("Keller")));
        PantryItem wine = PantryItem.create(new ItemName("Wein"), new Quantity(BigDecimal.ONE, Unit.PIECE), null,
                cellar.getStorageLocationId());

        repository.save(wine);

        assertThat(repository.findById(wine.getPantryItemId()).orElseThrow().getStorageLocationId())
                .isEqualTo(cellar.getStorageLocationId());
    }

    @Test
    void itemInUnknownLocationIsRejectedByDatabase() {
        PantryItem lost = PantryItem.create(new ItemName("Mehl"), new Quantity(BigDecimal.ONE, Unit.KILOGRAM), null,
                StorageLocationId.newId());

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> {
                    repository.save(lost);
                    repository.findAll();
                });
    }

    @Test
    void itemWithoutExpiryDateCanBeStored() {
        PantryItem salt = PantryItem.create(new ItemName("Salz"), new Quantity(BigDecimal.ONE, Unit.PACKAGE), null,
                StorageLocationId.DEFAULT);
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
        assertThat(repository.findAll()).hasSize(1);
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
        PantryItem milk = PantryItem.create(new ItemName("Milch"), new Quantity(BigDecimal.ONE, Unit.LITER),
                LocalDate.of(2027, 2, 24), StorageLocationId.DEFAULT);
        PantryItem flour = PantryItem.create(new ItemName("Mehl"), new Quantity(BigDecimal.ONE, Unit.KILOGRAM),
                LocalDate.of(2027, 6, 1), StorageLocationId.DEFAULT);
        PantryItem salt = PantryItem.create(new ItemName("Salz"), new Quantity(BigDecimal.ONE, Unit.PACKAGE), null,
                StorageLocationId.DEFAULT);
        repository.save(rice);
        repository.save(milk);
        repository.save(flour);
        repository.save(salt);

        assertThat(repository.findWithExpiryDateUpTo(LocalDate.of(2027, 3, 1)))
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
        StorageLocation cellar = storageLocationRepository.save(StorageLocation.create(new LocationName("Keller")));
        PantryItem wine = PantryItem.create(new ItemName("Wein"), new Quantity(BigDecimal.ONE, Unit.PIECE), null,
                cellar.getStorageLocationId());
        wine.changeMinimumAmount(new BigDecimal("6"));
        PantryItem juice = PantryItem.create(new ItemName("Apfelsaft"), new Quantity(BigDecimal.TEN, Unit.LITER), null,
                cellar.getStorageLocationId());
        juice.changeMinimumAmount(new BigDecimal("2"));
        rice.changeMinimumAmount(new BigDecimal("5"));
        repository.save(wine);
        repository.save(juice);
        repository.save(rice);

        assertThat(names(repository.search(PantryItemFilter.none(), new PageQuery(0, 10))))
                .containsExactly("Apfelsaft", "Reis", "Wein");
        assertThat(names(repository.search(new PantryItemFilter(cellar.getStorageLocationId(), false),
                new PageQuery(0, 10))))
                .containsExactly("Apfelsaft", "Wein");
        assertThat(names(repository.search(new PantryItemFilter(null, true), new PageQuery(0, 10))))
                .containsExactly("Reis", "Wein");
        assertThat(names(repository.search(new PantryItemFilter(cellar.getStorageLocationId(), true),
                new PageQuery(0, 10))))
                .containsExactly("Wein");
    }

    @Test
    void searchReturnsRequestedPage() {
        for (String name : List.of("A", "B", "C", "D", "E")) {
            repository.save(PantryItem.create(new ItemName(name), new Quantity(BigDecimal.ONE, Unit.PIECE), null,
                    StorageLocationId.DEFAULT));
        }

        ResultPage<PantryItem> secondPage = repository.search(PantryItemFilter.none(), new PageQuery(1, 2));

        assertThat(names(secondPage)).containsExactly("C", "D");
        assertThat(secondPage.totalElements()).isEqualTo(5);
        assertThat(secondPage.totalPages()).isEqualTo(3);
    }

    private static List<String> names(ResultPage<PantryItem> page) {
        return page.content().stream().map(item -> item.getItemName().value()).toList();
    }
}
