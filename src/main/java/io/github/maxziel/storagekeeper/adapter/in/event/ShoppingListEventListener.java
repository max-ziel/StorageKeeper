package io.github.maxziel.storagekeeper.adapter.in.event;

import io.github.maxziel.storagekeeper.application.port.in.UpdateShoppingListUseCase;
import io.github.maxziel.storagekeeper.domain.PantryItemFellBelowMinimum;
import io.github.maxziel.storagekeeper.domain.PantryItemReachedMinimum;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Driving adapter that turns domain events into shopping list updates. Like a REST controller,
 * it only translates and calls a use case; the rules live in the application and domain.
 */
@Component
class ShoppingListEventListener {

    private final UpdateShoppingListUseCase updateShoppingList;

    ShoppingListEventListener(UpdateShoppingListUseCase updateShoppingList) {
        this.updateShoppingList = updateShoppingList;
    }

    @EventListener
    void on(PantryItemFellBelowMinimum event) {
        updateShoppingList.addItem(event.pantryItemId());
    }

    @EventListener
    void on(PantryItemReachedMinimum event) {
        updateShoppingList.removeItem(event.pantryItemId());
    }
}
