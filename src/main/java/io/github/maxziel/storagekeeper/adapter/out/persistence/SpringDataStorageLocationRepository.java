package io.github.maxziel.storagekeeper.adapter.out.persistence;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data generates the implementation at runtime. Only used by {@link JpaStorageLocationRepository}. */
interface SpringDataStorageLocationRepository extends JpaRepository<StorageLocationJpaEntity, UUID> {

    // Spring Data derives the queries from the method names.

    List<StorageLocationJpaEntity> findByHouseholdId(UUID householdId, Sort sort);

    Optional<StorageLocationJpaEntity> findByHouseholdIdAndDefaultLocationTrue(UUID householdId);

    boolean existsByHouseholdIdAndNameIgnoreCase(UUID householdId, String name);
}
