package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.in.AddManualShoppingListEntryUseCase;
import io.github.maxziel.storagekeeper.application.port.in.CheckOffStockPurchaseUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ListShoppingListUseCase;
import io.github.maxziel.storagekeeper.application.port.in.RemoveManualShoppingListEntryUseCase;
import io.github.maxziel.storagekeeper.application.port.in.RestockPantryItemUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ShoppingList;
import io.github.maxziel.storagekeeper.application.port.in.ShoppingListItem;
import io.github.maxziel.storagekeeper.application.port.in.UpdateShoppingListUseCase;
import io.github.maxziel.storagekeeper.application.port.out.ManualShoppingListRepository;
import io.github.maxziel.storagekeeper.application.port.out.PantryItemRepository;
import io.github.maxziel.storagekeeper.application.port.out.ShoppingListRepository;
import io.github.maxziel.storagekeeper.domain.ItemName;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntry;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntryId;
import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;
import io.github.maxziel.storagekeeper.domain.Quantity;
import io.github.maxziel.storagekeeper.domain.ShoppingListEntry;
import io.github.maxziel.storagekeeper.domain.ShoppingListEntryId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Implements the shopping list use cases. */
@Service
public class ShoppingListService implements ListShoppingListUseCase, UpdateShoppingListUseCase,
        CheckOffStockPurchaseUseCase, AddManualShoppingListEntryUseCase, RemoveManualShoppingListEntryUseCase {

    private final ShoppingListRepository shoppingListRepository;
    private final ManualShoppingListRepository manualShoppingListRepository;
    private final PantryItemRepository pantryItemRepository;
    private final RestockPantryItemUseCase restockPantryItem;
    private final Clock clock;

    public ShoppingListService(ShoppingListRepository shoppingListRepository,
                               ManualShoppingListRepository manualShoppingListRepository,
                               PantryItemRepository pantryItemRepository,
                               RestockPantryItemUseCase restockPantryItem,
                               Clock clock) {
        this.shoppingListRepository =
                Objects.requireNonNull(shoppingListRepository, "shoppingListRepository must not be null");
        this.manualShoppingListRepository =
                Objects.requireNonNull(manualShoppingListRepository, "manualShoppingListRepository must not be null");
        this.pantryItemRepository = Objects.requireNonNull(pantryItemRepository, "pantryItemRepository must not be null");
        this.restockPantryItem = Objects.requireNonNull(restockPantryItem, "restockPantryItem must not be null");
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

    /**
     * Only restocks the item. Whether the entry disappears is decided by the item itself: if it reaches
     * its minimum, it raises {@code PantryItemReachedMinimum} and the listener calls {@link #removeItem}.
     */
    @Override
    @Transactional
    public PantryItem checkOff(ShoppingListEntryId shoppingListEntryId, BigDecimal purchasedAmount) {
        ShoppingListEntry entry = shoppingListRepository.findById(shoppingListEntryId)
                .orElseThrow(() -> new ShoppingListEntryNotFoundException(shoppingListEntryId));
        return restockPantryItem.restock(entry.getPantryItemId(), purchasedAmount);
    }

    @Override
    @Transactional
    public ManualShoppingListEntry add(ItemName name, Quantity quantity) {
        return manualShoppingListRepository.save(ManualShoppingListEntry.create(name, quantity, clock.instant()));
    }

    @Override
    @Transactional
    public void remove(ManualShoppingListEntryId manualShoppingListEntryId) {
        if (!manualShoppingListRepository.deleteById(manualShoppingListEntryId)) {
            throw new ShoppingListEntryNotFoundException(manualShoppingListEntryId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ShoppingList getShoppingList() {
        return new ShoppingList(listStockPurchases(), manualShoppingListRepository.findAll());
    }

    private List<ShoppingListItem> listStockPurchases() {
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