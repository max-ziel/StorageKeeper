package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.in.AddPantryItemCommand;
import io.github.maxziel.storagekeeper.application.port.in.ExpiringPantryItem;
import io.github.maxziel.storagekeeper.application.port.out.PantryItemRepository;

import io.github.maxziel.storagekeeper.application.port.out.StorageLocationRepository;
import io.github.maxziel.storagekeeper.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PantryItemServiceTest {

    @Mock
    private PantryItemRepository repository;

    @Mock
    private StorageLocationRepository storageLocationRepository;

    /** "Today" is always 26 February 2027 in these tests. */
    private final Clock clock = Clock.fixed(Instant.parse("2027-02-26T10:00:00Z"), ZoneOffset.UTC);

    private PantryItemService service;

    @BeforeEach
    void setUp() {
        service = new PantryItemService(repository, storageLocationRepository, clock);
    }

    private final PantryItem rice = PantryItem.create(
            new ItemName("Reis"), new Quantity(BigDecimal.ONE, Unit.KILOGRAM), LocalDate.of(2027, 3, 1),
            StorageLocationId.DEFAULT);

    @Test
    void addWithoutLocationUsesDefaultLocation() {
        when(storageLocationRepository.existsById(StorageLocationId.DEFAULT)).thenReturn(true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var command = new AddPantryItemCommand(rice.getItemName(), rice.getQuantity(), null, null);

        PantryItem added = service.add(command);

        assertThat(added.getItemName()).isEqualTo(rice.getItemName());
        assertThat(added.getQuantity()).isEqualTo(rice.getQuantity());
        assertThat(added.getStorageLocationId()).isEqualTo(StorageLocationId.DEFAULT);
        verify(repository).save(added);
    }

    @Test
    void addWithGivenLocationUsesIt() {
        StorageLocationId cellar = StorageLocationId.newId();
        when(storageLocationRepository.existsById(cellar)).thenReturn(true);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PantryItem added = service.add(new AddPantryItemCommand(rice.getItemName(), rice.getQuantity(), null, cellar));

        assertThat(added.getStorageLocationId()).isEqualTo(cellar);
    }

    @Test
    void addWithUnknownLocationFails() {
        StorageLocationId unknown = StorageLocationId.newId();
        when(storageLocationRepository.existsById(unknown)).thenReturn(false);
        var command = new AddPantryItemCommand(rice.getItemName(), rice.getQuantity(), null, unknown);

        assertThatExceptionOfType(StorageLocationNotFoundException.class)
                .isThrownBy(() -> service.add(command));
        verify(repository, never()).save(any());
    }

    @Test
    void changeQuantityUpdatesAndSavesItem() {
        when(repository.findById(rice.getPantryItemId())).thenReturn(Optional.of(rice));
        when(repository.save(rice)).thenReturn(rice);

        PantryItem changed = service.changeQuantity(rice.getPantryItemId(), new BigDecimal("0.5"));

        assertThat(changed.getQuantity()).isEqualTo(new Quantity(new BigDecimal("0.5"), Unit.KILOGRAM));
    }

    @Test
    void changeQuantityOfUnknownItemFails() {
        PantryItemId unknown = PantryItemId.newId();
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatExceptionOfType(PantryItemNotFoundException.class)
                .isThrownBy(() -> service.changeQuantity(unknown, BigDecimal.ONE));
    }

    @Test
    void removeOfUnknownItemFails() {
        PantryItemId unknown = PantryItemId.newId();
        when(repository.deleteById(unknown)).thenReturn(false);

        assertThatExceptionOfType(PantryItemNotFoundException.class)
                .isThrownBy(() -> service.remove(unknown));
    }

    @Test
    void listExpiringWithinLooksAheadFromToday() {
        when(repository.findWithExpiryDateUpTo(LocalDate.of(2027, 3, 5))).thenReturn(List.of(rice));

        List<ExpiringPantryItem> expiring = service.listExpiringWithin(7);

        assertThat(expiring).singleElement().satisfies(entry -> {
            assertThat(entry.item()).isEqualTo(rice);
            assertThat(entry.daysUntilExpiry()).isEqualTo(3);
            assertThat(entry.expired()).isFalse();
        });
    }

    @Test
    void listExpiringWithinMarksExpiredItems() {
        PantryItem oldMilk = PantryItem.create(new ItemName("Milch"), new Quantity(BigDecimal.ONE, Unit.LITER),
                LocalDate.of(2027, 2, 24), StorageLocationId.DEFAULT);
        when(repository.findWithExpiryDateUpTo(LocalDate.of(2027, 2, 26))).thenReturn(List.of(oldMilk));

        List<ExpiringPantryItem> expiring = service.listExpiringWithin(0);

        assertThat(expiring).singleElement().satisfies(entry -> {
            assertThat(entry.daysUntilExpiry()).isEqualTo(-2);
            assertThat(entry.expired()).isTrue();
        });
    }

    @Test
    void listExpiringWithinRejectsNegativeDays() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.listExpiringWithin(-1));
    }
}
