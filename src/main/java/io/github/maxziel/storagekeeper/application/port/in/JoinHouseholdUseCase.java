package io.github.maxziel.storagekeeper.application.port.in;

import io.github.maxziel.storagekeeper.domain.Household;
import io.github.maxziel.storagekeeper.domain.InviteCode;

/** Lets the current user join an existing household. */
public interface JoinHouseholdUseCase {

    /**
     * @return the household the user is now a member of
     * @throws io.github.maxziel.storagekeeper.application.UnknownInviteCodeException if no household has this code
     * @throws io.github.maxziel.storagekeeper.application.AlreadyInHouseholdException
     *         if the user already is a member of a household
     */
    Household join(InviteCode inviteCode);
}
