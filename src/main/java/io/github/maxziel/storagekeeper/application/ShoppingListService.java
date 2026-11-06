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
import io.github.maxziel.storagekeeper.domain.HouseholdId;
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

/**
 * Implements the shopping list use cases. The use cases for users work on the current user's household only;
 * entries of other households count as not found. {@link UpdateShoppingListUseCase} is called by the event
 * listener after a change that was already checked, so it does not check again.
 */
@Service
public class ShoppingListService implements ListShoppingListUseCase, UpdateShoppingListUseCase,
        CheckOffStockPurchaseUseCase, AddManualShoppingListEntryUseCase, RemoveManualShoppingListEntryUseCase {

    private final ShoppingListRepository shoppingListRepository;
    private final ManualShoppingListRepository manualShoppingListRepository;
    private final PantryItemRepository pantryItemRepository;
    private final RestockPantryItemUseCase restockPantryItem;
    private final Clock clock;
    private final CurrentHousehold currentHousehold;

    public ShoppingListService(ShoppingListRepository shoppingListRepository,
                               ManualShoppingListRepository manualShoppingListRepository,
                               PantryItemRepository pantryItemRepository,
                               RestockPantryItemUseCase restockPantryItem,
                               Clock clock,
                               CurrentHousehold currentHousehold) {
        this.shoppingListRepository =
                Objects.requireNonNull(shoppingListRepository, "shoppingListRepository must not be null");
        this.manualShoppingListRepository =
                Objects.requireNonNull(manualShoppingListRepository, "manualShoppingListRepository must not be null");
        this.pantryItemRepository = Objects.requireNonNull(pantryItemRepository, "pantryItemRepository must not be null");
        this.restockPantryItem = Objects.requireNonNull(restockPantryItem, "restockPantryItem must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.currentHousehold = Objects.requireNonNull(currentHousehold, "currentHousehold must not be null");
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
        HouseholdId household = currentHousehold.require();
        // The entry belongs to the household of its item.
        ShoppingListEntry entry = shoppingListRepository.findById(shoppingListEntryId)
                .filter(found -> pantryItemRepository.findById(found.getPantryItemId())
                        .map(item -> item.belongsTo(household))
                        .orElse(false))
                .orElseThrow(() -> new ShoppingListEntryNotFoundException(shoppingListEntryId));
        return restockPantryItem.restock(entry.getPantryItemId(), purchasedAmount);
    }

    @Override
    @Transactional
    public ManualShoppingListEntry add(ItemName name, Quantity quantity) {
        return manualShoppingListRepository.save(
                ManualShoppingListEntry.create(currentHousehold.require(), name, quantity, clock.instant()));
    }

    @Override
    @Transactional
    public void remove(ManualShoppingListEntryId manualShoppingListEntryId) {
        HouseholdId household = currentHousehold.require();
        manualShoppingListRepository.findById(manualShoppingListEntryId)
                .filter(entry -> entry.belongsTo(household))
                .orElseThrow(() -> new ShoppingListEntryNotFoundException(manualShoppingListEntryId));
        manualShoppingListRepository.deleteById(manualShoppingListEntryId);
    }

    @Override
    @Transactional(readOnly = true)
    public ShoppingList getShoppingList() {
        HouseholdId household = currentHousehold.require();
        return new ShoppingList(listStockPurchases(household), manualShoppingListRepository.findAll(household));
    }

    private List<ShoppingListItem> listStockPurchases(HouseholdId household) {
        List<ShoppingListEntry> entries = shoppingListRepository.findAll(household);
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