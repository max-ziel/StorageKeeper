package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.in.ListShoppingListUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ShoppingListItem;
import io.github.maxziel.storagekeeper.application.port.in.UpdateShoppingListUseCase;
import io.github.maxziel.storagekeeper.application.port.out.PantryItemRepository;
import io.github.maxziel.storagekeeper.application.port.out.ShoppingListRepository;
import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;
import io.github.maxziel.storagekeeper.domain.ShoppingListEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Implements the shopping list use cases. */
@Service
public class ShoppingListService implements ListShoppingListUseCase, UpdateShoppingListUseCase {

    private final ShoppingListRepository shoppingListRepository;
    private final PantryItemRepository pantryItemRepository;
    private final Clock clock;

    public ShoppingListService(ShoppingListRepository shoppingListRepository,
                               PantryItemRepository pantryItemRepository, Clock clock) {
        this.shoppingListRepository =
                Objects.requireNonNull(shoppingListRepository, "shoppingListRepository must not be null");
        this.pantryItemRepository = Objects.requireNonNull(pantryItemRepository, "pantryItemRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public void addItem(PantryItemId pantryItemId) {
        if (!shoppingListRepository.existsByPantryItemId(pantryItemId)) {
            shoppingListRepository.save(ShoppingListEntry.create(pantryItemId, clock.instant()));
        }
    }

    @Override
    @Transactional
    public void removeItem(PantryItemId pantryItemId) {
        shoppingListRepository.deleteByPantryItemId(pantryItemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShoppingListItem> listEntries() {
        List<ShoppingListEntry> entries = shoppingListRepository.findAll();
        // One query for all items instead of one per entry.
        Map<PantryItemId, PantryItem> items = pantryItemRepository
                .findAllById(entries.stream().map(ShoppingListEntry::getPantryItemId).toList())
                .stream()
                .collect(Collectors.toMap(PantryItem::getPantryItemId, Function.identity()));
        return entries.stream()
                .filter(entry -> items.containsKey(entry.getPantryItemId()))
                .map(entry -> new ShoppingListItem(entry, items.get(entry.getPantryItemId())))
                .toList();
    }
}
