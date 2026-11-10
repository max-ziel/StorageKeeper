package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.domain.PantryItemId;
import io.github.maxziel.storagekeeper.domain.ShoppingListEntry;
import io.github.maxziel.storagekeeper.domain.ShoppingListEntryId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Database row of table {@code shopping_list_entry}. The table is created by Flyway.
 */
@Entity
@Table(name = "shopping_list_entry")
class ShoppingListEntryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "pantry_item_id", nullable = false, unique = true)
    private UUID pantryItemId;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    /**
     * Required by JPA.
     */
    protected ShoppingListEntryJpaEntity() {
    }

    static ShoppingListEntryJpaEntity fromDomain(ShoppingListEntry entry) {
        ShoppingListEntryJpaEntity entity = new ShoppingListEntryJpaEntity();
        entity.id = entry.getShoppingListEntryId().value();
        entity.pantryItemId = entry.getPantryItemId().value();
        entity.addedAt = entry.getAddedAt();
        return entity;
    }

    ShoppingListEntry toDomain() {
        return new ShoppingListEntry(new ShoppingListEntryId(id), new PantryItemId(pantryItemId), addedAt);
    }
}
