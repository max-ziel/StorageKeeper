package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.Household;

/**
 * Shows the current user's household.
 */
public interface ShowHouseholdUseCase {

    /**
     * @throws io.github.maxziel.storagekeeper.application.NoHouseholdException if the user is not a member of any household
     */
    Household getCurrent();
}
