package io.github.maxziel.storagekeeper.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * An item that should be bought. Entity and aggregate root of its own; it refers to the
 * {@link PantryItem} only by id. There is at most one entry per item.
 */
public class ShoppingListEntry {

    private final ShoppingListEntryId shoppingListEntryId;
    private final PantryItemId pantryItemId;
    private final Instant addedAt;

    /** Restores an entry with a known id, e.g. when loading it from storage. Use {@link #create} for new entries. */
    public ShoppingListEntry(ShoppingListEntryId shoppingListEntryId, PantryItemId pantryItemId, Instant addedAt) {
        this.shoppingListEntryId = Objects.requireNonNull(shoppingListEntryId, "id must not be null");
        this.pantryItemId = Objects.requireNonNull(pantryItemId, "pantry item must not be null");
        this.addedAt = Objects.requireNonNull(addedAt, "addedAt must not be null");
    }

    /** Creates a new entry with a freshly generated id. */
    public static ShoppingListEntry create(PantryItemId pantryItemId, Instant now) {
        return new ShoppingListEntry(ShoppingListEntryId.newId(), pantryItemId, now);
    }

    public ShoppingListEntryId getShoppingListEntryId() {
        return shoppingListEntryId;
    }

    public PantryItemId getPantryItemId() {
        return pantryItemId;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ShoppingListEntry other && shoppingListEntryId.equals(other.shoppingListEntryId);
    }

    @Override
    public int hashCode() {
        return shoppingListEntryId.hashCode();
    }
}
