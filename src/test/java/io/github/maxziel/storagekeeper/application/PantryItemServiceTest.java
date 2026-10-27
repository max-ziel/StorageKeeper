package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.in.AddPantryItemCommand;
import io.github.maxziel.storagekeeper.application.port.out.PantryItemRepository;

import io.github.maxziel.storagekeeper.application.port.out.StorageLocationRepository;
import io.github.maxziel.storagekeeper.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @InjectMocks
    private PantryItemService service;

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
}
