package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.domain.*;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Database row of table {@code manual_shopping_list_entry}. The table is created by Flyway.
 */
@Entity
@Table(name = "manual_shopping_list_entry")
class ManualShoppingListEntryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(nullable = false, length = ItemName.MAX_LENGTH)
    private String name;

    /**
     * {@code null} together with {@link #unit} if no amount was given.
     */
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Unit unit;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    /**
     * Required by JPA.
     */
    protected ManualShoppingListEntryJpaEntity() {
    }

    static ManualShoppingListEntryJpaEntity fromDomain(ManualShoppingListEntry entry) {
        ManualShoppingListEntryJpaEntity entity = new ManualShoppingListEntryJpaEntity();
        entity.id = entry.getManualShoppingListEntryId().value();
        entity.householdId = entry.getHouseholdId().value();
        entity.name = entry.getName().value();
        entity.amount = entry.getQuantity().map(Quantity::amount).orElse(null);
        entity.unit = entry.getQuantity().map(Quantity::unit).orElse(null);
        entity.addedAt = entry.getAddedAt();
        return entity;
    }

    ManualShoppingListEntry toDomain() {
        return new ManualShoppingListEntry(
            new ManualShoppingListEntryId(id),
            new HouseholdId(householdId),
            new ItemName(name),
            amount == null ? null : new Quantity(amount, unit),
            addedAt);
    }
}
