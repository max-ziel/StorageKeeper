package io.github.maxziel.storagekeeper.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Something the user wrote on the shopping list by hand, e.g. "Geburtstagskerzen".
 * Unlike a {@link ShoppingListEntry}, it is not linked to a {@link PantryItem}: checking it off
 * simply removes it, the stock does not change. It belongs to exactly one {@link Household}.
 *
 * <p>Invariants: name and time of adding are set; the quantity is optional, but if given its amount is positive.
 */
public class ManualShoppingListEntry {

    private final ManualShoppingListEntryId manualShoppingListEntryId;
    private final HouseholdId householdId;
    private final ItemName name;
    private final Quantity quantity;
    private final Instant addedAt;

    /**
     * Restores an entry with a known id, e.g. when loading it from storage. Use {@link #create} for new entries.
     */
    public ManualShoppingListEntry(
            ManualShoppingListEntryId manualShoppingListEntryId,
            HouseholdId householdId,
            ItemName name,
            Quantity quantity,
            Instant addedAt
    ) {
        this.manualShoppingListEntryId = Objects.requireNonNull(manualShoppingListEntryId, "id must not be null");
        this.householdId = Objects.requireNonNull(householdId, "household must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.addedAt = Objects.requireNonNull(addedAt, "addedAt must not be null");
        if (quantity != null && quantity.amount().signum() <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        this.quantity = quantity;
    }

    /**
     * Creates a new entry with a freshly generated id.
     *
     * @param quantity optional, {@code null} if the user did not say how much
     */
    public static ManualShoppingListEntry create(HouseholdId householdId, ItemName name, Quantity quantity,
                                                 Instant now) {
        return new ManualShoppingListEntry(ManualShoppingListEntryId.newId(), householdId, name, quantity, now);
    }

    public ManualShoppingListEntryId getManualShoppingListEntryId() {
        return manualShoppingListEntryId;
    }

    public HouseholdId getHouseholdId() {
        return householdId;
    }

    /** {@code true} if the entry is on this household's shopping list. */
    public boolean belongsTo(HouseholdId household) {
        return householdId.equals(household);
    }

    public ItemName getName() {
        return name;
    }

    public Optional<Quantity> getQuantity() {
        return Optional.ofNullable(quantity);
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ManualShoppingListEntry other
                && manualShoppingListEntryId.equals(other.manualShoppingListEntryId);
    }

    @Override
    public int hashCode() {
        return manualShoppingListEntryId.hashCode();
    }
}
