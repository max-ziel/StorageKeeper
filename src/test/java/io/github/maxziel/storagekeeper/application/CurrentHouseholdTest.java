package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.out.CurrentUser;
import io.github.maxziel.storagekeeper.application.port.out.CurrentUserProvider;
import io.github.maxziel.storagekeeper.application.port.out.HouseholdRepository;
import io.github.maxziel.storagekeeper.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentHouseholdTest {

    private static final CurrentUser ALICE = new CurrentUser(new UserId("id-alice"), "alice");

    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private HouseholdRepository householdRepository;
    @InjectMocks
    private CurrentHousehold currentHousehold;

    @Test
    void returnsTheHouseholdOfTheCurrentUser() {
        Household household = Household.found(new HouseholdName("WG"),
                new HouseholdMember(ALICE.userId(), "alice"), InviteCode.generate());
        when(currentUserProvider.currentUser()).thenReturn(ALICE);
        when(householdRepository.findByMember(ALICE.userId())).thenReturn(Optional.of(household));

        assertThat(currentHousehold.require()).isEqualTo(household.getHouseholdId());
    }

    @Test
    void failsIfTheUserHasNoHousehold() {
        when(currentUserProvider.currentUser()).thenReturn(ALICE);
        when(householdRepository.findByMember(ALICE.userId())).thenReturn(Optional.empty());

        assertThatExceptionOfType(NoHouseholdException.class).isThrownBy(() -> currentHousehold.require());
    }
}
