package io.github.maxziel.storagekeeper.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * A product stored in the pantry.
 * Entity and aggregate root: it has an identity
 * ({@link PantryItemId}) and is compared by that identity only, while its state
 * (quantity) can change over time.
 */
public class PantryItem {

    private final PantryItemId pantryItemId;
    private final ItemName itemName;
    private Quantity quantity;
    private final LocalDate expiryDate;
    private final StorageLocationId storageLocationId;
    private BigDecimal minimumAmount;

    public PantryItem(PantryItemId pantryItemId, ItemName itemName, Quantity quantity, LocalDate expiryDate, StorageLocationId storageLocationId) {
        this(pantryItemId, itemName, quantity, expiryDate, storageLocationId, BigDecimal.ZERO);
    }

    /**
     * Restores an item including its minimum stock.
     *
     * @param minimumAmount minimum stock in the item's unit; 0 means "no minimum"
     */
    public PantryItem(PantryItemId pantryItemId, ItemName itemName, Quantity quantity, LocalDate expiryDate,
                      StorageLocationId storageLocationId, BigDecimal minimumAmount) {
        this.pantryItemId = Objects.requireNonNull(pantryItemId, "id must not be null");
        this.itemName = Objects.requireNonNull(itemName, "name must not be null");
        this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
        this.expiryDate = expiryDate;
        this.storageLocationId = Objects.requireNonNull(storageLocationId, "storage location must not be null");
        changeMinimumAmount(minimumAmount);
    }

    /** Creates a new item with a freshly generated id. */
    public static PantryItem create(ItemName itemName, Quantity quantity, LocalDate expiryDate, StorageLocationId storageLocationId) {
        return new PantryItem(PantryItemId.newId(), itemName, quantity, expiryDate, storageLocationId);
    }

    /**
     * Sets hte minimum stock in the item's unit. 0 means "no minimum".
     *
     * @throws IllegalArgumentException if the amount is negative
     */
    public void changeMinimumAmount(BigDecimal newMinimumAmount) {
        // Quantity checks "not null, not negative"
        this.minimumAmount = quantity.withAmount(newMinimumAmount).amount();
    }

    /**
     *
     * @return {@code true} if the current amount is below the minimum stock, i.e. the item should be bought.
     */
    public boolean isBelowMinimum() {
        return quantity.amount().compareTo(minimumAmount) < 0;
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

    public StorageLocationId getStorageLocationId() {
        return storageLocationId;
    }

    public Quantity getMinimumQuantity() {
        return quantity.withAmount(minimumAmount);
    }

    /**
     * Days from {@code today} until the expiry date: 0 means it expires today,
     * negative values mean it has already expired.
     *
     * @throws IllegalStateException if the item has no expiry date
     */
    public long daysUntilExpiry(LocalDate today) {
        if (expiryDate == null) {
            throw new IllegalStateException("item " + pantryItemId + " has no expiry date");
        }

        return ChronoUnit.DAYS.between(today, expiryDate);
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
