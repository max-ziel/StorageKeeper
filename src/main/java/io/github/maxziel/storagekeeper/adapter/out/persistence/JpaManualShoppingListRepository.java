package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.application.port.out.ManualShoppingListRepository;
import io.github.maxziel.storagekeeper.domain.HouseholdId;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntry;
import io.github.maxziel.storagekeeper.domain.ManualShoppingListEntryId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
    public List<ManualShoppingListEntry> findAll(HouseholdId householdId) {
        return jpaRepository.findByHouseholdIdOrderByAddedAtAsc(householdId.value())
                .stream()
                .map(ManualShoppingListEntryJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<ManualShoppingListEntry> findById(ManualShoppingListEntryId manualShoppingListEntryId) {
        return jpaRepository.findById(manualShoppingListEntryId.value()).map(ManualShoppingListEntryJpaEntity::toDomain);
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
