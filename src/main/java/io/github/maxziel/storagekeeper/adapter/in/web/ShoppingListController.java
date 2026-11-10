package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.port.in.AddManualShoppingListEntryUseCase;
import io.github.maxziel.storagekeeper.application.port.in.CheckOffStockPurchaseUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ListShoppingListUseCase;
import io.github.maxziel.storagekeeper.application.port.in.RemoveManualShoppingListEntryUseCase;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntry;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntryId;
import io.github.maxziel.storagekeeper.domain.ShoppingListEntryId;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/**
 * REST endpoints for the shopping list. Stock purchases are created and removed automatically from the
 * stock and can only be checked off; manual entries are written and removed by the user.
 */
@RestController
@Tag(name = "Einkaufsliste", description = "Vorratseinkäufe abhaken und manuelle Einträge pflegen")
@RequestMapping("/api/shopping-list")
class ShoppingListController {

    private final ListShoppingListUseCase listShoppingList;
    private final CheckOffStockPurchaseUseCase checkOffStockPurchase;
    private final AddManualShoppingListEntryUseCase addManualEntry;
    private final RemoveManualShoppingListEntryUseCase removeManualEntry;

    ShoppingListController(
        ListShoppingListUseCase listShoppingList,
        CheckOffStockPurchaseUseCase checkOffStockPurchase,
        AddManualShoppingListEntryUseCase addManualEntry,
        RemoveManualShoppingListEntryUseCase removeManualEntry
    ) {
        this.listShoppingList = listShoppingList;
        this.checkOffStockPurchase = checkOffStockPurchase;
        this.addManualEntry = addManualEntry;
        this.removeManualEntry = removeManualEntry;
    }

    @GetMapping
    ShoppingListResponse list() {
        return ShoppingListResponse.from(listShoppingList.getShoppingList());
    }

    /**
     * Adds the bought amount to the item and returns the item with its new stock.
     */
    @PostMapping("/stock-purchases/{id}/check-off")
    PantryItemResponse checkOff(@PathVariable UUID id, @Valid @RequestBody CheckOffStockPurchaseRequest request) {
        return PantryItemResponse.from(checkOffStockPurchase.checkOff(new ShoppingListEntryId(id), request.amount()));
    }

    @PostMapping("/manual-entries")
    ResponseEntity<ManualShoppingListEntryResponse> addManualEntry(
        @Valid @RequestBody AddManualShoppingListEntryRequest request) {
        ManualShoppingListEntry entry = addManualEntry.add(request.toName(), request.toQuantity());
        URI uri = URI.create("/api/shopping-list/manual-entries/" + entry.getManualShoppingListEntryId());
        return ResponseEntity.created(uri).body(ManualShoppingListEntryResponse.from(entry));
    }

    /**
     * Checking off a manual entry simply removes it.
     */
    @DeleteMapping("/manual-entries/{id}")
    ResponseEntity<Void> removeManualEntry(@PathVariable UUID id) {
        removeManualEntry.remove(new ManualShoppingListEntryId(id));
        return ResponseEntity.noContent().build();
    }
}
