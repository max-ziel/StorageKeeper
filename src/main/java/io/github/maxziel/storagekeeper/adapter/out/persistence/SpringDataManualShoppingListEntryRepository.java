package io.github.maxziel.storagekeeper.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data generates the implementation at runtime. Only used by {@link JpaManualShoppingListRepository}. */
interface SpringDataManualShoppingListEntryRepository extends JpaRepository<ManualShoppingListEntryJpaEntity, UUID> {

    List<ManualShoppingListEntryJpaEntity> findAllByOrderByAddedAtAsc();
}
