package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.TestcontainersConfiguration;
import io.github.maxziel.storagekeeper.domain.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

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

    private final PantryItem rice = PantryItem.create(
            new ItemName("Reis"), new Quantity(BigDecimal.ONE, Unit.KILOGRAM), null, StorageLocationId.DEFAULT);
    private final PantryItem salt = PantryItem.create(
            new ItemName("Salz"), new Quantity(BigDecimal.ONE, Unit.PACKAGE), null, StorageLocationId.DEFAULT);

    @Test
    void savedEntriesAreListedOldestFirst() {
        pantryItemRepository.save(rice);
        pantryItemRepository.save(salt);
        ShoppingListEntry later = repository.save(ShoppingListEntry.create(rice.getPantryItemId(),
                Instant.parse("2027-02-26T12:00:00Z")));
        ShoppingListEntry earlier = repository.save(ShoppingListEntry.create(salt.getPantryItemId(),
                Instant.parse("2027-02-26T08:00:00Z")));

        assertThat(repository.findAll()).containsExactly(earlier, later);
        assertThat(repository.existsByPantryItemId(rice.getPantryItemId())).isTrue();
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

        assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() -> {
            repository.save(ShoppingListEntry.create(rice.getPantryItemId(), Instant.now()));
            repository.findAll();
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