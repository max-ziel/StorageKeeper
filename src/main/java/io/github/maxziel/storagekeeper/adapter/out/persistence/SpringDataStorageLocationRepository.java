package io.github.maxziel.storagekeeper.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Spring Data generates the implementation at runtime. Only used by {@link JpaStorageLocationRepository}. */
interface SpringDataStorageLocationRepository extends JpaRepository<StorageLocationJpaEntity, UUID> {

    /** Spring Data derives the query from the method name. */
    boolean existsByNameIgnoreCase(String name);
}
