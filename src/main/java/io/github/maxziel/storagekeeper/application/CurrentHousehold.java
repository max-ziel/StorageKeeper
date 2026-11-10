package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.out.CurrentUserProvider;
import io.github.maxziel.storagekeeper.application.port.out.HouseholdRepository;
import io.github.maxziel.storagekeeper.domain.Household;
import io.github.maxziel.storagekeeper.domain.HouseholdId;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Finds the household of the user who sent the current request. Every use case on pantry data asks this first
 * and then only reads and changes data of that household: this is where the authorization lives.
 */
@Component
public class CurrentHousehold {

    private final CurrentUserProvider currentUserProvider;
    private final HouseholdRepository householdRepository;

    public CurrentHousehold(CurrentUserProvider currentUserProvider, HouseholdRepository householdRepository) {
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider, "currentUserProvider must not be null");
        this.householdRepository = Objects.requireNonNull(householdRepository, "householdRepository must not be null");
    }

    /**
     * @throws NoHouseholdException if the current user is not a member of any household
     */
    public HouseholdId require() {
        return householdRepository.findByMember(currentUserProvider.currentUser().userId())
                .map(Household::getHouseholdId)
                .orElseThrow(NoHouseholdException::new);
    }
}
