package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.Household;
import io.github.maxziel.storagekeeper.domain.HouseholdName;

/**
 * Creates a household for the current user.
 */
public interface CreateHouseholdUseCase {

    /**
     * Creates the household with a new invite code and its default storage location; the current user becomes
     * its first member.
     *
     * @throws io.github.maxziel.storagekeeper.application.AlreadyInHouseholdException if the user already is a member of a household
     */
    Household create(HouseholdName name);
}
