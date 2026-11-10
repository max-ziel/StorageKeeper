package io.github.maxziel.storagekeeper.application;

import io.github.maxziel.storagekeeper.application.port.out.CurrentUser;
import io.github.maxziel.storagekeeper.application.port.out.CurrentUserProvider;
import io.github.maxziel.storagekeeper.application.port.out.HouseholdRepository;
import io.github.maxziel.storagekeeper.application.port.out.StorageLocationRepository;
import io.github.maxziel.storagekeeper.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HouseholdServiceTest {

    private static final CurrentUser BOB = new CurrentUser(new UserId("id-bob"), "bob");
    private static final HouseholdMember ALICE = new HouseholdMember(new UserId("id-alice"), "alice");

    @Mock
    private HouseholdRepository householdRepository;
    @Mock
    private StorageLocationRepository storageLocationRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private HouseholdService service;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserProvider.currentUser()).thenReturn(BOB);
        lenient().when(householdRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new HouseholdService(householdRepository, storageLocationRepository, currentUserProvider);
    }

    @Test
    void createMakesTheUserTheFirstMemberAndAddsADefaultLocation() {
        when(householdRepository.findByMember(BOB.userId())).thenReturn(Optional.empty());
        when(householdRepository.findByInviteCode(any())).thenReturn(Optional.empty());

        Household created = service.create(new HouseholdName("WG"));

        assertThat(created.getName()).isEqualTo(new HouseholdName("WG"));
        assertThat(created.getMembers()).containsExactly(new HouseholdMember(BOB.userId(), "bob"));
        verify(householdRepository).save(created);

        ArgumentCaptor<StorageLocation> location = ArgumentCaptor.forClass(StorageLocation.class);
        verify(storageLocationRepository).save(location.capture());
        assertThat(location.getValue().isDefault()).isTrue();
        assertThat(location.getValue().belongsTo(created.getHouseholdId())).isTrue();
    }

    @Test
    void createDrawsANewCodeIfTheFirstOneIsTaken() {
        Household other = Household.found(new HouseholdName("Andere"), ALICE, InviteCode.generate());
        when(householdRepository.findByMember(BOB.userId())).thenReturn(Optional.empty());
        when(householdRepository.findByInviteCode(any()))
            .thenReturn(Optional.of(other))
            .thenReturn(Optional.empty());

        Household created = service.create(new HouseholdName("WG"));

        verify(householdRepository, times(2)).findByInviteCode(any());
        assertThat(created.getInviteCode()).isNotNull();
    }

    @Test
    void createWhenAlreadyInAHouseholdFails() {
        Household existing = Household.found(new HouseholdName("WG"), ALICE, InviteCode.generate());
        existing.join(new HouseholdMember(BOB.userId(), "bob"));
        when(householdRepository.findByMember(BOB.userId())).thenReturn(Optional.of(existing));

        assertThatExceptionOfType(AlreadyInHouseholdException.class)
            .isThrownBy(() -> service.create(new HouseholdName("Zweiter")));
        verify(householdRepository, never()).save(any());
        verifyNoInteractions(storageLocationRepository);
    }

    @Test
    void getCurrentReturnsTheUsersHousehold() {
        Household household = Household.found(new HouseholdName("WG"), ALICE, InviteCode.generate());
        when(householdRepository.findByMember(BOB.userId())).thenReturn(Optional.of(household));

        assertThat(service.getCurrent()).isEqualTo(household);
    }

    @Test
    void getCurrentWithoutHouseholdFails() {
        when(householdRepository.findByMember(BOB.userId())).thenReturn(Optional.empty());

        assertThatExceptionOfType(NoHouseholdException.class).isThrownBy(() -> service.getCurrent());
    }

    @Test
    void joinAddsTheUserToTheHouseholdWithThisCode() {
        InviteCode code = InviteCode.generate();
        Household household = Household.found(new HouseholdName("WG"), ALICE, code);
        when(householdRepository.findByMember(BOB.userId())).thenReturn(Optional.empty());
        when(householdRepository.findByInviteCode(code)).thenReturn(Optional.of(household));

        Household joined = service.join(code);

        assertThat(joined.getMembers()).containsExactly(ALICE, new HouseholdMember(BOB.userId(), "bob"));
        verify(householdRepository).save(household);
    }

    @Test
    void joinWithUnknownCodeFails() {
        when(householdRepository.findByMember(BOB.userId())).thenReturn(Optional.empty());
        when(householdRepository.findByInviteCode(any())).thenReturn(Optional.empty());

        assertThatExceptionOfType(UnknownInviteCodeException.class)
            .isThrownBy(() -> service.join(InviteCode.generate()));
        verify(householdRepository, never()).save(any());
    }

    @Test
    void joinWhenAlreadyInAHouseholdFails() {
        Household own = Household.found(new HouseholdName("Eigener"), new HouseholdMember(BOB.userId(), "bob"),
            InviteCode.generate());
        when(householdRepository.findByMember(BOB.userId())).thenReturn(Optional.of(own));

        assertThatExceptionOfType(AlreadyInHouseholdException.class)
            .isThrownBy(() -> service.join(InviteCode.generate()));
        verify(householdRepository, never()).findByInviteCode(any());
        verify(householdRepository, never()).save(any());
    }
}
