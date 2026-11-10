package io.github.maxziel.storagekeeper.application.port.out;

import io.github.maxziel.storagekeeper.domain.Household;
import io.github.maxziel.storagekeeper.domain.InviteCode;
import io.github.maxziel.storagekeeper.domain.UserId;

import java.util.Optional;

/**
 * Storage for households and their members, as needed by the application.
 */
public interface HouseholdRepository {

    /**
     * Inserts or replaces the household together with its members.
     */
    Household save(Household household);

    /**
     * The household the user is a member of; a user is in at most one.
     */
    Optional<Household> findByMember(UserId userId);

    Optional<Household> findByInviteCode(InviteCode inviteCode);
}
