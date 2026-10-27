package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.application.port.out.PantryItemRepository;
import io.github.maxziel.storagekeeper.domain.PantryItem;
import io.github.maxziel.storagekeeper.domain.PantryItemId;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implements the {@link PantryItemRepository} port with PostgreSQL via Spring Data JPA.
 * Translates between the domain class {@link PantryItem} and {@link PantryItemJpaEntity}.
 */
@Repository
class JpaPantryItemRepository implements PantryItemRepository {

    private final SpringDataPantryItemRepository jpaRepository;

    JpaPantryItemRepository(SpringDataPantryItemRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }


    @Override
    public PantryItem save(PantryItem pantryItem) {
        jpaRepository.save(PantryItemJpaEntity.fromDomain(pantryItem));
        return pantryItem;
    }

    @Override
    public Optional<PantryItem> findById(PantryItemId pantryItemId) {
        return jpaRepository.findById(pantryItemId.value())
                .map(PantryItemJpaEntity::toDomain);
    }

    @Override
    public List<PantryItem> findAll() {
        return jpaRepository.findAll().stream()
                .map(PantryItemJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public boolean deleteById(PantryItemId pantryItemId) {
        if (!jpaRepository.existsById(pantryItemId.value())) {
            return false;
        }

        jpaRepository.deleteById(pantryItemId.value());
        return  true;
    }
}
