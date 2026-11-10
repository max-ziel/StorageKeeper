package io.github.maxziel.storagekeeper.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data generates the implementation at runtime. Only used by {@link JpaShoppingListRepository}.
 */
interface SpringDataShoppingListEntryRepository extends JpaRepository<ShoppingListEntryJpaEntity, UUID> {

    boolean existsByPantryItemId(UUID pantryItemId);

    void deleteByPantryItemId(UUID pantryItemId);

    /**
     * Entries have no household column; they belong to the household of their item.
     */
    @Query("""
        select e from ShoppingListEntryJpaEntity e
        where e.pantryItemId in (select p.id from PantryItemJpaEntity p where p.householdId = :householdId)
        order by e.addedAt
        """)
    List<ShoppingListEntryJpaEntity> findAllForHousehold(@Param("householdId") UUID householdId);
}
