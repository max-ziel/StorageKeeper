package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.in.ShoppingListItem;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingListServiceTest {

    private static final Instant NOW = Instant.parse("2027-02-26T10:00:00Z");

    @Mock
    private ShoppingListRepository shoppingListRepository;

    @Mock
    private PantryItemRepository pantryItemRepository;

    private ShoppingListService service;

    private final PantryItem rice = PantryItem.create(
            new ItemName("Reis"), new Quantity(BigDecimal.ONE, Unit.KILOGRAM), null, StorageLocationId.DEFAULT);

    @BeforeEach
    void setUp() {
        service = new ShoppingListService(shoppingListRepository, pantryItemRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
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
    void listEntriesCombinesEntriesWithTheirItems() {
        ShoppingListEntry entry = ShoppingListEntry.create(rice.getPantryItemId(), NOW);
        ShoppingListEntry orphan = ShoppingListEntry.create(PantryItemId.newId(), NOW);
        when(shoppingListRepository.findAll()).thenReturn(List.of(entry, orphan));
        when(pantryItemRepository.findAllById(List.of(rice.getPantryItemId(), orphan.getPantryItemId())))
                .thenReturn(List.of(rice));

        List<ShoppingListItem> items = service.listEntries();

        assertThat(items).containsExactly(new ShoppingListItem(entry, rice));
    }
}