package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.TestcontainersConfiguration;
import io.github.maxziel.storagekeeper.domain.ItemName;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntry;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntryId;
import io.github.maxziel.storagekeeper.domain.Quantity;
import io.github.maxziel.storagekeeper.domain.Unit;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaManualShoppingListRepository.class})
class JpaManualShoppingListRepositoryTest {

    @Autowired
    private JpaManualShoppingListRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savedEntriesAreListedOldestFirstWithOrWithoutQuantity() {
        ManualShoppingListEntry later = repository.save(ManualShoppingListEntry.create(new ItemName("Kerzen"),
                new Quantity(new BigDecimal("12"), Unit.PIECE), Instant.parse("2027-02-26T12:00:00Z")));
        ManualShoppingListEntry earlier = repository.save(ManualShoppingListEntry.create(new ItemName("Ballons"),
                null, Instant.parse("2027-02-26T08:00:00Z")));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAll()).containsExactly(earlier, later);
        ManualShoppingListEntry loaded = repository.findAll().get(1);
        assertThat(loaded.getQuantity()).hasValueSatisfying(quantity -> {
            assertThat(quantity.amount()).isEqualByComparingTo("12");
            assertThat(quantity.unit()).isEqualTo(Unit.PIECE);
        });
        assertThat(repository.findAll().get(0).getQuantity()).isEmpty();
    }

    @Test
    void deleteByIdReportsWhetherTheEntryExisted() {
        ManualShoppingListEntry entry = repository.save(
                ManualShoppingListEntry.create(new ItemName("Kerzen"), null, Instant.now()));

        assertThat(repository.deleteById(entry.getManualShoppingListEntryId())).isTrue();
        assertThat(repository.deleteById(ManualShoppingListEntryId.newId())).isFalse();
        assertThat(repository.findAll()).isEmpty();
    }
}