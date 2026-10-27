package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.TestcontainersConfiguration;
import io.github.maxziel.storagekeeper.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaPantryItemRepository.class})
class JpaPantryItemRepositoryTest {

    @Autowired
    private JpaPantryItemRepository repository;

    private final PantryItem rice = PantryItem.create(
            new ItemName("Reis"),
            new Quantity(new BigDecimal("1.5"), Unit.KILOGRAM),
            LocalDate.of(2027,3,1)
    );

    @Test
    void savedItemCanBeLoadedWithAllFields() {
        repository.save(rice);

        PantryItem loaded = repository.findById(rice.getPantryItemId()).orElseThrow();

        assertThat(loaded).isEqualTo(rice);
        assertThat(loaded.getItemName()).isEqualTo(rice.getItemName());
        assertThat(loaded.getQuantity()).isEqualTo(rice.getQuantity());
        assertThat(loaded.getExpiryDate()).isEqualTo(rice.getExpiryDate());
    }

    @Test
    void itemWithoutExpiryDateCanBeStored() {
        PantryItem salt = PantryItem.create(new ItemName("Salz"), new Quantity(BigDecimal.ONE, Unit.PACKAGE), null);
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
}
