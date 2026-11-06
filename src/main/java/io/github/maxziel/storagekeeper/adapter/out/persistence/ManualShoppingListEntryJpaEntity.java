package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.domain.HouseholdId;
import io.github.maxziel.storagekeeper.domain.ItemName;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntry;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntryId;
import io.github.maxziel.storagekeeper.domain.Quantity;
import io.github.maxziel.storagekeeper.domain.Unit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Database row of table {@code manual_shopping_list_entry}. The table is created by Flyway. */
@Entity
@Table(name = "manual_shopping_list_entry")
class ManualShoppingListEntryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(nullable = false, length = 100)
    private String name;

    /** {@code null} together with {@link #unit} if no amount was given. */
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Unit unit;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    /** Required by JPA. */
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