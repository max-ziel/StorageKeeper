package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.port.in.ListShoppingListUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** REST endpoints for the shopping list. Entries are created and removed automatically from the stock. */
@RestController
@RequestMapping("/api/shopping-list")
class ShoppingListController {

    private final ListShoppingListUseCase listShoppingList;

    ShoppingListController(ListShoppingListUseCase listShoppingList) {
        this.listShoppingList = listShoppingList;
    }

    @GetMapping
    List<ShoppingListEntryResponse> list() {
        return listShoppingList.listEntries().stream()
                .map(ShoppingListEntryResponse::from)
                .toList();
    }
}
