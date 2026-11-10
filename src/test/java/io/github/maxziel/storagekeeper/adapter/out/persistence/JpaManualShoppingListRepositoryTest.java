package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.TestcontainersConfiguration;
import io.github.maxziel.storagekeeper.adapter.out.persistence.TestHouseholds.StoredHousehold;
import io.github.maxziel.storagekeeper.domain.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaManualShoppingListRepository.class})
class JpaManualShoppingListRepositoryTest {

    @Autowired
    private JpaManualShoppingListRepository repository;

    @Autowired
    private EntityManager entityManager;

    private StoredHousehold household;

    @BeforeEach
    void setUp() {
        household = TestHouseholds.create(entityManager);
    }

    @Test
    void savedEntriesAreListedOldestFirstWithOrWithoutQuantity() {
        ManualShoppingListEntry later = repository.save(ManualShoppingListEntry.create(household.id(),
            new ItemName("Kerzen"), new Quantity(new BigDecimal("12"), Unit.PIECE),
            Instant.parse("2027-02-26T12:00:00Z")));
        ManualShoppingListEntry earlier = repository.save(ManualShoppingListEntry.create(household.id(),
            new ItemName("Ballons"), null, Instant.parse("2027-02-26T08:00:00Z")));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAll(household.id())).containsExactly(earlier, later);
        ManualShoppingListEntry loaded = repository.findAll(household.id()).get(1);
        assertThat(loaded.getHouseholdId()).isEqualTo(household.id());
        assertThat(loaded.getQuantity()).hasValueSatisfying(quantity -> {
            assertThat(quantity.amount()).isEqualByComparingTo("12");
            assertThat(quantity.unit()).isEqualTo(Unit.PIECE);
        });
        assertThat(repository.findAll(household.id()).getFirst().getQuantity()).isEmpty();
    }

    @Test
    void findAllOnlyReturnsEntriesOfTheHousehold() {
        StoredHousehold otherHousehold = TestHouseholds.create(entityManager);
        ManualShoppingListEntry own = repository.save(
            ManualShoppingListEntry.create(household.id(), new ItemName("Kerzen"), null, Instant.now()));
        repository.save(ManualShoppingListEntry.create(otherHousehold.id(), new ItemName("Ballons"), null, Instant.now()));

        assertThat(repository.findAll(household.id())).containsExactly(own);
    }

    @Test
    void findByIdAndDeleteById() {
        ManualShoppingListEntry entry = repository.save(
            ManualShoppingListEntry.create(household.id(), new ItemName("Kerzen"), null, Instant.now()));

        assertThat(repository.findById(entry.getManualShoppingListEntryId())).contains(entry);
        assertThat(repository.deleteById(entry.getManualShoppingListEntryId())).isTrue();
        assertThat(repository.deleteById(ManualShoppingListEntryId.newId())).isFalse();
        assertThat(repository.findAll(household.id())).isEmpty();
    }
}
