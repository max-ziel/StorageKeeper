package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.application.port.out.ManualShoppingListRepository;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntry;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntryId;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Implements the {@link io.github.maxziel.storagekeeper.application.port.out.ManualShoppingListRepository} port
 * with PostgreSQL via Spring Data JPA.
 */
@Repository
class JpaManualShoppingListRepository implements ManualShoppingListRepository {

    private final SpringDataManualShoppingListEntryRepository jpaRepository;

    JpaManualShoppingListRepository(SpringDataManualShoppingListEntryRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ManualShoppingListEntry save(ManualShoppingListEntry entry) {
        jpaRepository.save(ManualShoppingListEntryJpaEntity.fromDomain(entry));

        return entry;
    }

    @Override
    public List<ManualShoppingListEntry> findAll() {
        return jpaRepository.findAllByOrderByAddedAtAsc()
                .stream()
                .map(ManualShoppingListEntryJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean deleteById(ManualShoppingListEntryId manualShoppingListEntryId) {
        if (!jpaRepository.existsById(manualShoppingListEntryId.value())) {
            return false;
        }

        jpaRepository.deleteById(manualShoppingListEntryId.value());
        return true;
    }
}
