package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.in.RestockPantryItemUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ShoppingList;
import io.github.maxziel.storagekeeper.application.port.in.ShoppingListItem;
import io.github.maxziel.storagekeeper.application.port.out.ManualShoppingListRepository;
import io.github.maxziel.storagekeeper.application.port.out.PantryItemRepository;
import io.github.maxziel.storagekeeper.application.port.out.ShoppingListRepository;
import io.github.maxziel.storagekeeper.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {

    private static final Instant NOW = Instant.parse("2027-02-26T10:00:00Z");

    @Mock
    private ShoppingListRepository shoppingListRepository;

    @Mock
    private ManualShoppingListRepository manualShoppingListRepository;

    @Mock
    private PantryItemRepository pantryItemRepository;

    @Mock
    private RestockPantryItemUseCase restockPantryItem;

    private ShoppingListService service;

    private final PantryItem rice = PantryItem.create(
            new ItemName("Reis"), new Quantity(BigDecimal.ONE, Unit.KILOGRAM), null, StorageLocationId.DEFAULT);

    @BeforeEach
    void setUp() {
        service = new ShoppingListService(shoppingListRepository, manualShoppingListRepository, pantryItemRepository,
                restockPantryItem, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void addItemCreatesEntry() {
        when(shoppingListRepository.existsByPantryItemId(rice.getPantryItemId())).thenReturn(false);

        service.addItem(rice.getPantryItemId());

        ArgumentCaptor<ShoppingListEntry> saved = ArgumentCaptor.forClass(ShoppingListEntry.class);
        verify(shoppingListRepository).save(saved.capture());
        assertThat(saved.getValue().getPantryItemId()).isEqualTo(rice.getPantryItemId());
        assertThat(saved.getValue().getAddedAt()).isEqualTo(NOW);
    }

    @Test
    void addItemDoesNothingIfAlreadyOnTheList() {
        when(shoppingListRepository.existsByPantryItemId(rice.getPantryItemId())).thenReturn(true);

        service.addItem(rice.getPantryItemId());

        verify(shoppingListRepository, never()).save(any());
    }

    @Test
    void removeItemDeletesEntry() {
        service.removeItem(rice.getPantryItemId());

        verify(shoppingListRepository).deleteByPantryItemId(rice.getPantryItemId());
    }

    @Test
    void shoppingListCombinesEntriesWithTheirItemsAndAddsManualEntries() {
        ShoppingListEntry entry = ShoppingListEntry.create(rice.getPantryItemId(), NOW);
        ShoppingListEntry orphan = ShoppingListEntry.create(PantryItemId.newId(), NOW);
        ManualShoppingListEntry candles = ManualShoppingListEntry.create(new ItemName("Kerzen"), null, NOW);
        when(shoppingListRepository.findAll()).thenReturn(List.of(entry, orphan));
        when(pantryItemRepository.findAllById(List.of(rice.getPantryItemId(), orphan.getPantryItemId())))
                .thenReturn(List.of(rice));
        when(manualShoppingListRepository.findAll()).thenReturn(List.of(candles));

        ShoppingList list = service.getShoppingList();

        assertThat(list.stockPurchases()).containsExactly(new ShoppingListItem(entry, rice));
        assertThat(list.manualEntries()).containsExactly(candles);
    }

    @Test
    void checkOffRestocksTheItemOfTheEntry() {
        ShoppingListEntry entry = ShoppingListEntry.create(rice.getPantryItemId(), NOW);
        when(shoppingListRepository.findById(entry.getShoppingListEntryId())).thenReturn(Optional.of(entry));
        when(restockPantryItem.restock(rice.getPantryItemId(), BigDecimal.TWO)).thenReturn(rice);

        PantryItem restocked = service.checkOff(entry.getShoppingListEntryId(), BigDecimal.TWO);

        assertThat(restocked).isEqualTo(rice);
        // The entry is removed by the event listener, not here.
        verify(shoppingListRepository, never()).deleteByPantryItemId(any());
    }

    @Test
    void checkOffOfUnknownEntryFails() {
        ShoppingListEntryId unknown = ShoppingListEntryId.newId();
        when(shoppingListRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ShoppingListEntryNotFoundException.class)
                .isThrownBy(() -> service.checkOff(unknown, BigDecimal.ONE));
        verifyNoInteractions(restockPantryItem);
    }

    @Test
    void addManualEntrySavesIt() {
        Quantity twelve = new Quantity(new BigDecimal("12"), Unit.PIECE);
        when(manualShoppingListRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ManualShoppingListEntry added = service.add(new ItemName("Kerzen"), twelve);

        assertThat(added.getName()).isEqualTo(new ItemName("Kerzen"));
        assertThat(added.getQuantity()).contains(twelve);
        assertThat(added.getAddedAt()).isEqualTo(NOW);
        verify(manualShoppingListRepository).save(added);
    }

    @Test
    void removeManualEntryDeletesIt() {
        ManualShoppingListEntryId id = ManualShoppingListEntryId.newId();
        when(manualShoppingListRepository.deleteById(id)).thenReturn(true);

        service.remove(id);

        verify(manualShoppingListRepository).deleteById(id);
    }

    @Test
    void removeOfUnknownManualEntryFails() {
        ManualShoppingListEntryId unknown = ManualShoppingListEntryId.newId();
        when(manualShoppingListRepository.deleteById(unknown)).thenReturn(false);

        assertThatExceptionOfType(ShoppingListEntryNotFoundException.class)
                .isThrownBy(() -> service.remove(unknown));
    }
}