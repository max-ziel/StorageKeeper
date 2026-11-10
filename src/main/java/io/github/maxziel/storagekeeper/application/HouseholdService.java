package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.in.CreateHouseholdUseCase;
import io.github.maxziel.storagekeeper.application.port.in.JoinHouseholdUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ShowHouseholdUseCase;
import io.github.maxziel.storagekeeper.application.port.out.CurrentUser;
import io.github.maxziel.storagekeeper.application.port.out.CurrentUserProvider;
import io.github.maxziel.storagekeeper.application.port.out.HouseholdRepository;
import io.github.maxziel.storagekeeper.application.port.out.StorageLocationRepository;
import io.github.maxziel.storagekeeper.domain.Household;
import io.github.maxziel.storagekeeper.domain.HouseholdMember;
import io.github.maxziel.storagekeeper.domain.HouseholdName;
import io.github.maxziel.storagekeeper.domain.InviteCode;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/** Implements the household use cases. */
@Service
public class HouseholdService implements CreateHouseholdUseCase, ShowHouseholdUseCase, JoinHouseholdUseCase {

    private static final Logger log = LoggerFactory.getLogger(HouseholdService.class);
    private final HouseholdRepository householdRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final CurrentUserProvider currentUserProvider;

    public HouseholdService(HouseholdRepository householdRepository,
                            StorageLocationRepository storageLocationRepository,
                            CurrentUserProvider currentUserProvider) {
        this.householdRepository = Objects.requireNonNull(householdRepository, "householdRepository must not be null");
        this.storageLocationRepository =
                Objects.requireNonNull(storageLocationRepository, "storageLocationRepository must not be null");
        this.currentUserProvider = Objects.requireNonNull(currentUserProvider, "currentUserProvider must not be null");
    }

    @Override
    @Transactional
    public Household create(HouseholdName name) {
        CurrentUser user = currentUserProvider.currentUser();
        requireNoHousehold(user);

        Household household = householdRepository.save(Household.found(name, asMember(user), newInviteCode()));
        storageLocationRepository.save(StorageLocation.createDefault(household.getHouseholdId()));

        log.info("Household {} founded", household.getHouseholdId().value());
        return household;
    }

    @Override
    @Transactional(readOnly = true)
    public Household getCurrent() {
        return householdRepository.findByMember(currentUserProvider.currentUser().userId())
                .orElseThrow(NoHouseholdException::new);
    }

    @Override
    @Transactional
    public Household join(InviteCode inviteCode) {
        CurrentUser user = currentUserProvider.currentUser();
        requireNoHousehold(user);

        Household household = householdRepository.findByInviteCode(inviteCode)
                .orElseThrow(UnknownInviteCodeException::new);
        household.join(asMember(user));

        Household saved = householdRepository.save(household);
        log.info("User joined household {}, which now has {} members",
                saved.getHouseholdId().value(), saved.getMembers().size());
        return saved;
    }

    private void requireNoHousehold(CurrentUser user) {
        if (householdRepository.findByMember(user.userId()).isPresent()) {
            throw new AlreadyInHouseholdException();
        }
    }

    /** Codes are unique; a collision is very unlikely, but costs only one more lookup. */
    private InviteCode newInviteCode() {
        InviteCode code;
        do {
            code = InviteCode.generate();
        } while (householdRepository.findByInviteCode(code).isPresent());
        return code;
    }

    private static HouseholdMember asMember(CurrentUser user) {
        return new HouseholdMember(user.userId(), user.displayName());
    }
}
