package io.github.maxziel.storagekeeper.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data generates the implementation at runtime. Only used by {@link JpaPantryItemRepository}. */
interface SpringDataPantryItemRepository extends JpaRepository<PantryItemJpaEntity, UUID> {
}
