package io.github.maxziel.storagekeeper.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data generates the implementation at runtime. Only used by {@link JpaHouseholdRepository}.
 */
interface SpringDataHouseholdRepository extends JpaRepository<HouseholdJpaEntity, UUID> {

    @Query("select h from HouseholdJpaEntity h join h.members m where m.userId = :userId")
    Optional<HouseholdJpaEntity> findByMemberUserId(@Param("userId") String userId);

    Optional<HouseholdJpaEntity> findByInviteCode(String inviteCode);
}
