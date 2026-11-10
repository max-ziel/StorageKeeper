package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.application.port.out.HouseholdRepository;
import io.github.maxziel.storagekeeper.domain.Household;
import io.github.maxziel.storagekeeper.domain.InviteCode;
import io.github.maxziel.storagekeeper.domain.UserId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Implements the {@link HouseholdRepository} port with PostgreSQL via Spring Data JPA.
 */
@Repository
class JpaHouseholdRepository implements HouseholdRepository {

    private final SpringDataHouseholdRepository jpaRepository;

    JpaHouseholdRepository(SpringDataHouseholdRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Household save(Household household) {
        jpaRepository.save(HouseholdJpaEntity.fromDomain(household));
        return household;
    }

    @Override
    public Optional<Household> findByMember(UserId userId) {
        return jpaRepository.findByMemberUserId(userId.value()).map(HouseholdJpaEntity::toDomain);
    }

    @Override
    public Optional<Household> findByInviteCode(InviteCode inviteCode) {
        return jpaRepository.findByInviteCode(inviteCode.value()).map(HouseholdJpaEntity::toDomain);
    }
}
