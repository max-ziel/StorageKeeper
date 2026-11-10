package io.github.maxziel.storagekeeper.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A product stored in the pantry.
 * Entity and aggregate root: it has an identity
 * ({@link PantryItemId}) and is compared by that identity only, while its state
 * (quantity) can change over time. It belongs to exactly one {@link Household}, which never changes.
 */
public class PantryItem {

    private final PantryItemId pantryItemId;
    private final HouseholdId householdId;
    private final ItemName itemName;
    private Quantity quantity;
    private final LocalDate expiryDate;
    private final StorageLocationId storageLocationId;
    private BigDecimal minimumAmount;
    /** Events raised by changes since the item was loaded; collected by {@link #pullDomainEvents()}. */
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    public PantryItem(PantryItemId pantryItemId, HouseholdId householdId, ItemName itemName, Quantity quantity,
                      LocalDate expiryDate, StorageLocationId storageLocationId) {
        this(pantryItemId, householdId, itemName, quantity, expiryDate, storageLocationId, BigDecimal.ZERO);
    }

    /**
     * Restores an item including its minimum stock.
     *
     * @param minimumAmount minimum stock in the item's unit; 0 means "no minimum"
     */
    public PantryItem(PantryItemId pantryItemId, HouseholdId householdId, ItemName itemName, Quantity quantity,
                      LocalDate expiryDate, StorageLocationId storageLocationId, BigDecimal minimumAmount) {
        this.pantryItemId = Objects.requireNonNull(pantryItemId, "id must not be null");
        this.householdId = Objects.requireNonNull(householdId, "household must not be null");
        this.itemName = Objects.requireNonNull(itemName, "name must not be null");
        this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
        this.expiryDate = expiryDate;
        this.storageLocationId = Objects.requireNonNull(storageLocationId, "storage location must not be null");
        // Set directly instead of via changeMinimumAmount(): restoring an item is not a change and raises no event.
        this.minimumAmount = quantity.withAmount(minimumAmount).amount();
    }

    /** Creates a new item with a freshly generated id. */
    public static PantryItem create(HouseholdId householdId, ItemName itemName, Quantity quantity, LocalDate expiryDate,
                                    StorageLocationId storageLocationId) {
        return new PantryItem(PantryItemId.newId(), householdId, itemName, quantity, expiryDate, storageLocationId);
    }

    /**
     * Sets the minimum stock in the item's unit. 0 means "no minimum".
     *
     * @throws DomainValidationException if the amount is negative
     */
    public void changeMinimumAmount(BigDecimal newMinimumAmount) {
        boolean wasBelowMinimum = isBelowMinimum();
        // Quantity checks "not null, not negative"
        this.minimumAmount = quantity.withAmount(newMinimumAmount).amount();
        recordMinimumTransition(wasBelowMinimum);
    }

    /**
     * @return {@code true} if the current amount is below the minimum stock, i.e. the item should be bought.
     */
    public boolean isBelowMinimum() {
        return quantity.amount().compareTo(minimumAmount) < 0;
    }

    /** Sets a new amount; the unit stays the same. */
    public void changeQuantity(BigDecimal newAmount) {
        boolean wasBelowMinimum = isBelowMinimum();
        this.quantity = this.quantity.withAmount(newAmount);
        recordMinimumTransition(wasBelowMinimum);
    }

    /**
     * Adds a purchased amount to the stock, e.g. after shopping. Raises the same events as
     * {@link #changeQuantity(BigDecimal)}.
     *
     * @throws DomainValidationException if the amount is not positive
     */
    public void restock(BigDecimal purchasedAmount) {
        Objects.requireNonNull(purchasedAmount, "purchased amount must not be null");
        if (purchasedAmount.signum() <= 0) {
            throw new DomainValidationException("purchased amount must be positive");
        }

        changeQuantity(quantity.amount().add(purchasedAmount));
    }

    /**
     * How much is missing to reach the minimum stock, in the item's unit; zero if the item is not below its minimum.
     */
    public Quantity getMissingQuantity() {
        BigDecimal missing = minimumAmount.subtract(quantity.amount());
        return quantity.withAmount(missing.signum() > 0 ? missing : BigDecimal.ZERO);
    }

    /**
     * Returns the events raised since the last call and forgets them, so each event is handed out only once.
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    /** Raises an event only when the item crosses its minimum stock, not on every change. */
    private void recordMinimumTransition(boolean wasBelowMinimum) {
        boolean isBelowMinimum = isBelowMinimum();
        if (!wasBelowMinimum && isBelowMinimum) {
            domainEvents.add(new PantryItemFellBelowMinimum(pantryItemId));
        } else if (wasBelowMinimum && !isBelowMinimum) {
            domainEvents.add(new PantryItemReachedMinimum(pantryItemId));
        }
    }

    public PantryItemId getPantryItemId() {
        return pantryItemId;
    }

    public HouseholdId getHouseholdId() {
        return householdId;
    }

    /** {@code true} if the item is part of this household's pantry. */
    public boolean belongsTo(HouseholdId household) {
        return householdId.equals(household);
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
