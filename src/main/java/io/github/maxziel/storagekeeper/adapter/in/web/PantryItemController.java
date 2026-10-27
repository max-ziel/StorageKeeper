package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.port.in.AddPantryItemUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ChangeQuantityUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ListPantryItemUseCase;
import io.github.maxziel.storagekeeper.application.port.in.RemovePantryItemUseCase;
import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;
import jakarta.validation.Valid;
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
@RequestMapping("/api/pantry-items")
class PantryItemController {

    private final AddPantryItemUseCase addPantryItem;
    private final ListPantryItemUseCase listPantryItems;
    private final ChangeQuantityUseCase changeQuantity;
    private final RemovePantryItemUseCase removePantryItem;

    PantryItemController(
            AddPantryItemUseCase addPantryItem,
            ListPantryItemUseCase listPantryItems,
            ChangeQuantityUseCase changeQuantity,
            RemovePantryItemUseCase removePantryItem
    ) {
        this.addPantryItem = addPantryItem;
        this.listPantryItems = listPantryItems;
        this.changeQuantity = changeQuantity;
        this.removePantryItem = removePantryItem;
    }

    @PostMapping
    ResponseEntity<PantryItemResponse> add(@Valid @RequestBody AddPantryItemRequest request) {
        PantryItem item = addPantryItem.add(request.toCommand());
        URI location = URI.create("/api/pantry-items/" + item.getPantryItemId());

        return ResponseEntity.created(location).body(PantryItemResponse.from(item));
    }

    @GetMapping
    List<PantryItemResponse> listAll() {
        return listPantryItems.listAll().stream()
                .map(PantryItemResponse::from).toList();
    }

    @PatchMapping("/{id}/quantity")
    PantryItemResponse changeQuantity(@PathVariable UUID id, @Valid @RequestBody ChangeQuantityRequest request) {
        return PantryItemResponse.from(changeQuantity.changeQuantity(new PantryItemId(id), request.amount()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> remove(@PathVariable UUID id) {
        removePantryItem.remove(new PantryItemId(id));
        return ResponseEntity.noContent().build();
    }
}
