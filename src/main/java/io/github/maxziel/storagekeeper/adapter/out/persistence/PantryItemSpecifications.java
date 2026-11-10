package io.github.maxziel.storagekeeper.adapter.out.persistence;

import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Building blocks for filtered queries on {@link PantryItemJpaEntity}. Each method describes one
 * WHERE condition; {@link JpaPantryItemRepository} combines the ones that arte needed.
 */
public class PantryItemSpecifications {

    private PantryItemSpecifications() {
    }

    /** {@code household_id = ?} */
    static Specification<PantryItemJpaEntity> inHousehold(UUID householdId) {
        return (root, _, cb) ->
                cb.equal(root.get("householdId"), householdId);
    }

    /** {@code storage_location_id = ?} */
    static Specification<PantryItemJpaEntity> inStorageLocation(UUID storageLocationId) {
        return (root, _, cb) ->
                cb.equal(root.get("storageLocationId"), storageLocationId);
    }

    /** {@code amount < minimum_amount} */
    static Specification<PantryItemJpaEntity> belowMinimum() {
        return (root, _, cb) ->
                cb.lessThan(root.get("amount"), root.get("minimumAmount"));
    }
}
