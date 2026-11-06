package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.port.PageQuery;
import io.github.maxziel.storagekeeper.application.port.in.*;
import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;
import io.github.maxziel.storagekeeper.domain.StorageLocationId;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for pantry items. Translates HTTP requests into use case calls
 * and domain objects into JSON responses; contains no business logic itself.
 */
@RestController
@Tag(name = "Vorratsartikel", description = "Artikel im Vorrat anlegen, suchen, ändern und entfernen")
@RequestMapping("/api/pantry-items")
class PantryItemController {

    private final AddPantryItemUseCase addPantryItem;
    private final ListPantryItemUseCase listPantryItems;
    private final ChangeQuantityUseCase changeQuantity;
    private final RemovePantryItemUseCase removePantryItem;
    private final ListExpiringPantryItemsUseCase listExpiringPantryItems;
    private final ChangeMinimumAmountUseCase changeMinimumAmount;

    PantryItemController(
            AddPantryItemUseCase addPantryItem,
            ListPantryItemUseCase listPantryItems,
            ChangeQuantityUseCase changeQuantity,
            RemovePantryItemUseCase removePantryItem,
            ListExpiringPantryItemsUseCase listExpiringPantryItems,
            ChangeMinimumAmountUseCase changeMinimumAmount
    ) {
        this.addPantryItem = addPantryItem;
        this.listPantryItems = listPantryItems;
        this.changeQuantity = changeQuantity;
        this.removePantryItem = removePantryItem;
        this.listExpiringPantryItems = listExpiringPantryItems;
        this.changeMinimumAmount = changeMinimumAmount;
    }

    @PostMapping
    ResponseEntity<PantryItemResponse> add(@Valid @RequestBody AddPantryItemRequest request) {
        PantryItem item = addPantryItem.add(request.toCommand());
        URI location = URI.create("/api/pantry-items/" + item.getPantryItemId());

        return ResponseEntity.created(location).body(PantryItemResponse.from(item));
    }

    /**
     * One page of items, sorted by nema. All filters are optional and combined with AND.
     *
     * @param storageLocationId only items in this location
     * @param belowMinimum {@code true} = only items below their minimum
     */
    @GetMapping
    PageResponse<PantryItemResponse> list(
            @RequestParam(required = false) UUID storageLocationId,
            @RequestParam(defaultValue = "false") boolean belowMinimum,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size
    ) {
        PantryItemFilter filter = new PantryItemFilter(
                storageLocationId == null ? null : new StorageLocationId(storageLocationId),
                belowMinimum
        );

        return PageResponse.from(
                listPantryItems.search(filter, new PageQuery(page, size)).map(PantryItemResponse::from)
        );
    }

    /** Items that expire within the next {@code days} days or have already expired, earliest first. */
    @GetMapping("/expiring")
    List<ExpiringPantryItemResponse> listExpiring(@RequestParam(defaultValue = "7") @Min(0) @Max(365) int days) {
        return listExpiringPantryItems.listExpiringWithin(days).stream()
                .map(ExpiringPantryItemResponse::from)
                .toList();
    }

    @PatchMapping("/{id}/quantity")
    PantryItemResponse changeQuantity(@PathVariable UUID id, @Valid @RequestBody ChangeQuantityRequest request) {
        return PantryItemResponse.from(changeQuantity.changeQuantity(new PantryItemId(id), request.amount()));
    }

    @PatchMapping("/{id}/minimum")
    PantryItemResponse changeMinimumAmount(@PathVariable UUID id,
                                           @Valid @RequestBody ChangeMinimumAmountRequest request) {
        return PantryItemResponse.from(changeMinimumAmount.changeMinimumAmount(new PantryItemId(id), request.amount()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> remove(@PathVariable UUID id) {
        removePantryItem.remove(new PantryItemId(id));
        return ResponseEntity.noContent().build();
    }
}
