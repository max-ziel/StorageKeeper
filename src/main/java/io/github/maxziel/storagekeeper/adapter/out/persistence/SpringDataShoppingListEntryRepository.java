package io.github.maxziel.storagekeeper.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data generates the implementation at runtime. Only used by {@link JpaShoppingListRepository}. */
interface SpringDataShoppingListEntryRepository extends JpaRepository<ShoppingListEntryJpaEntity, UUID> {

    boolean existsByPantryItemId(UUID pantryItemId);

    void deleteByPantryItemId(UUID pantryItemId);

    List<ShoppingListEntryJpaEntity> findAllByOrderByAddedAtAsc();
}
