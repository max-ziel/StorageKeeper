package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.application.port.out.ShoppingListRepository;
import io.github.maxziel.storagekeeper.domain.PantryItemId;
import io.github.maxziel.storagekeeper.domain.ShoppingListEntry;
import io.github.maxziel.storagekeeper.domain.ShoppingListEntryId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Implements the {@link ShoppingListRepository} port with PostgreSQL via Spring Data JPA. */
@Repository
class JpaShoppingListRepository implements ShoppingListRepository {

    private final SpringDataShoppingListEntryRepository jpaRepository;

    JpaShoppingListRepository(SpringDataShoppingListEntryRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ShoppingListEntry save(ShoppingListEntry entry) {
        jpaRepository.save(ShoppingListEntryJpaEntity.fromDomain(entry));
        return entry;
    }

    @Override
    public List<ShoppingListEntry> findAll() {
        return jpaRepository.findAllByOrderByAddedAtAsc().stream()
                .map(ShoppingListEntryJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByPantryItemId(PantryItemId pantryItemId) {
        return jpaRepository.existsByPantryItemId(pantryItemId.value());
    }

    @Override
    @Transactional
    public void deleteByPantryItemId(PantryItemId pantryItemId) {
        jpaRepository.deleteByPantryItemId(pantryItemId.value());
    }

    @Override
    public Optional<ShoppingListEntry> findById(ShoppingListEntryId shoppingListEntryId) {
        return jpaRepository.findById(shoppingListEntryId.value()).map(ShoppingListEntryJpaEntity::toDomain);
    }
}
