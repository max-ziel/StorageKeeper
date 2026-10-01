package io.github.maxziel.storagekeeper.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public class PantryItem {

    private final PantryItemId pantryItemId;
    private final ItemName itemName;
    private Quantity quantity;
    private final LocalDate expiryDate;

    /**
     * Restores an item with a known id, e.g. when loading it from storage.
     * Use {@link #create} for new items.
     *
     * @param expiryDate best-before date, or {@code null} if the product has none
     */
    public PantryItem(PantryItemId pantryItemId, ItemName itemName, Quantity quantity, LocalDate expiryDate) {
        this.pantryItemId = Objects.requireNonNull(pantryItemId, "id must no be null");
        this.itemName = Objects.requireNonNull(itemName, "name must no be null");
        this.quantity = Objects.requireNonNull(quantity, "quantity must no be null");
        this.expiryDate = expiryDate;
    }

    /** Creates a new item with a feshly generated id. */
    public static PantryItem create(ItemName itemName, Quantity quantity, LocalDate expiryDate) {
        return new PantryItem(PantryItemId.newId(), itemName, quantity, expiryDate);
    }

    /** Sets a new amount; the unit stays the same. */
    public void changeQuantity(BigDecimal newAmount) {
        this.quantity = this.quantity.withAmount(newAmount);
    }

    public PantryItemId getPantryItemId() {
        return pantryItemId;
    }

    public ItemName getItemName() {
        return itemName;
    }

    public Quantity getQuantity() {
        return quantity;
    }

    public Optional<LocalDate> getExpiryDate() {
        return Optional.ofNullable(expiryDate);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PantryItem other && pantryItemId.equals(other.pantryItemId);
    }

    @Override
    public int hashCode() {
        return pantryItemId.hashCode();
    }
}
