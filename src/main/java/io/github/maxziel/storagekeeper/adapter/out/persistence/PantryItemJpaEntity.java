package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.domain.*;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Database row of table {@code pantry_item}. Kept separate from the domain class
 * {@link PantryItem} so that JPA requirements (no-arg constructor, mutable fields)
 * do not leak into the domain model. Flyway creates the table itself.
 */
@Entity
@Table(name = "pantry_item")
class PantryItemJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = ItemName.MAX_LENGTH)
    private String name;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Unit unit;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    /** Plain id instead of a JPA relation: items and locations are separate aggregates. */
    @Column(name = "storage_location_id", nullable = false)
    private UUID storageLocationId;

    /** Required by JPA. */
    protected PantryItemJpaEntity() {
    }

    static PantryItemJpaEntity fromDomain(PantryItem pantryItem) {
        PantryItemJpaEntity entity = new PantryItemJpaEntity();

        entity.id = pantryItem.getPantryItemId().value();
        entity.name = pantryItem.getItemName().value();
        entity.amount = pantryItem.getQuantity().amount();
        entity.unit = pantryItem.getQuantity().unit();
        entity.expiryDate = pantryItem.getExpiryDate().orElse(null);
        entity.storageLocationId = pantryItem.getStorageLocationId().value();

        return entity;
    }

    PantryItem toDomain() {
        return new PantryItem(
                new PantryItemId(id),
                new ItemName(name),
                new Quantity(amount, unit),
                expiryDate,
                new StorageLocationId(storageLocationId));
    }
}
