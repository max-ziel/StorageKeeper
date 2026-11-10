package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.TestcontainersConfiguration;
import io.github.maxziel.storagekeeper.adapter.out.persistence.TestHouseholds.StoredHousehold;
import io.github.maxziel.storagekeeper.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaShoppingListRepository.class, JpaPantryItemRepository.class})
class JpaShoppingListRepositoryTest {

    @Autowired
    private JpaShoppingListRepository repository;

    @Autowired
    private JpaPantryItemRepository pantryItemRepository;

    @Autowired
    private EntityManager entityManager;

    private StoredHousehold household;
    private PantryItem rice;
    private PantryItem salt;

    @BeforeEach
    void setUp() {
        household = TestHouseholds.create(entityManager);
        rice = PantryItem.create(household.id(), new ItemName("Reis"),
                new Quantity(BigDecimal.ONE, Unit.KILOGRAM), null, household.defaultLocation());
        salt = PantryItem.create(household.id(), new ItemName("Salz"),
                new Quantity(BigDecimal.ONE, Unit.PACKAGE), null, household.defaultLocation());
    }

    @Test
    void savedEntriesAreListedOldestFirst() {
        pantryItemRepository.save(rice);
        pantryItemRepository.save(salt);
        ShoppingListEntry later = repository.save(ShoppingListEntry.create(rice.getPantryItemId(),
                Instant.parse("2027-02-26T12:00:00Z")));
        ShoppingListEntry earlier = repository.save(ShoppingListEntry.create(salt.getPantryItemId(),
                Instant.parse("2027-02-26T08:00:00Z")));

        assertThat(repository.findAll(household.id())).containsExactly(earlier, later);
        assertThat(repository.existsByPantryItemId(rice.getPantryItemId())).isTrue();
    }

    @Test
    void findAllOnlyReturnsEntriesForItemsOfTheHousehold() {
        StoredHousehold otherHousehold = TestHouseholds.create(entityManager);
        PantryItem foreignRice = PantryItem.create(otherHousehold.id(), new ItemName("Reis"),
                new Quantity(BigDecimal.ONE, Unit.KILOGRAM), null, otherHousehold.defaultLocation());
        pantryItemRepository.save(rice);
        pantryItemRepository.save(foreignRice);
        ShoppingListEntry own = repository.save(ShoppingListEntry.create(rice.getPantryItemId(), Instant.now()));
        ShoppingListEntry foreign =
                repository.save(ShoppingListEntry.create(foreignRice.getPantryItemId(), Instant.now()));
        entityManager.flush();

        assertThat(repository.findAll(household.id())).containsExactly(own);
        assertThat(repository.findAll(otherHousehold.id())).containsExactly(foreign);
    }

    @Test
    void findByIdReturnsTheEntry() {
        pantryItemRepository.save(rice);
        ShoppingListEntry entry = repository.save(ShoppingListEntry.create(rice.getPantryItemId(), Instant.now()));

        assertThat(repository.findById(entry.getShoppingListEntryId())).contains(entry);
        assertThat(repository.findById(ShoppingListEntryId.newId())).isEmpty();
    }

    @Test
    void deleteByPantryItemIdRemovesTheEntry() {
        pantryItemRepository.save(rice);
        repository.save(ShoppingListEntry.create(rice.getPantryItemId(), Instant.now()));

        repository.deleteByPantryItemId(rice.getPantryItemId());

        assertThat(repository.existsByPantryItemId(rice.getPantryItemId())).isFalse();
    }

    @Test
    void onlyOneEntryPerItem() {
        pantryItemRepository.save(rice);
        repository.save(ShoppingListEntry.create(rice.getPantryItemId(), Instant.now()));

        assertThatExceptionOfType(PersistenceException.class).isThrownBy(() -> {
            repository.save(ShoppingListEntry.create(rice.getPantryItemId(), Instant.now()));
            entityManager.flush();
        });
    }

    @Test
    void deletingTheItemDeletesItsEntry() {
        pantryItemRepository.save(rice);
        repository.save(ShoppingListEntry.create(rice.getPantryItemId(), Instant.now()));
        entityManager.flush();
        entityManager.clear();

        pantryItemRepository.deleteById(rice.getPantryItemId());
        entityManager.flush();

        assertThat(repository.existsByPantryItemId(rice.getPantryItemId())).isFalse();
    }
}