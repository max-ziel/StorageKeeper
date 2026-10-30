package io.github.maxziel.storagekeeper.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Spring Data generates the implementation at runtime. Only used by {@link JpaPantryItemRepository}. */
interface SpringDataPantryItemRepository extends JpaRepository<PantryItemJpaEntity, UUID> {
    /** Spring Data derives the query from the method name: expiry_date <= ? ORDER BY expiry_date. */
    List<PantryItemJpaEntity> findByExpiryDateLessThanEqualOrderByExpiryDateAsc(LocalDate latest);
}
