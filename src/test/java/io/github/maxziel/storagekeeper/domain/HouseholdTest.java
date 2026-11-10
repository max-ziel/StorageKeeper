package io.github.maxziel.storagekeeper.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class HouseholdTest {

    private static final HouseholdMember ALICE = new HouseholdMember(new UserId("id-alice"), "alice");
    private static final HouseholdMember BOB = new HouseholdMember(new UserId("id-bob"), "bob");
    private static final InviteCode CODE = new InviteCode("K7M2-QX9P");

    @Test
    void founderIsTheFirstMember() {
        Household household = Household.found(new HouseholdName("WG"), ALICE, CODE);

        assertThat(household.getMembers()).containsExactly(ALICE);
        assertThat(household.hasMember(ALICE.userId())).isTrue();
        assertThat(household.hasMember(BOB.userId())).isFalse();
    }

    @Test
    void membersAreKeptInTheOrderTheyJoined() {
        Household household = Household.found(new HouseholdName("WG"), ALICE, CODE);

        household.join(BOB);

        assertThat(household.getMembers()).containsExactly(ALICE, BOB);
        assertThat(household.hasMember(BOB.userId())).isTrue();
    }

    @Test
    void aMemberCannotJoinTwice() {
        Household household = Household.found(new HouseholdName("WG"), ALICE, CODE);

        assertThatIllegalStateException()
            .isThrownBy(() -> household.join(new HouseholdMember(ALICE.userId(), "alice again")));
    }

    @Test
    void restoringWithTheSameUserTwiceIsRejected() {
        assertThatIllegalStateException().isThrownBy(() ->
            new Household(HouseholdId.newId(), new HouseholdName("WG"), CODE, List.of(ALICE, ALICE)));
    }

    @Test
    void householdWithoutMembersCanBeRestored() {
        Household migrated = new Household(HouseholdId.newId(), new HouseholdName("Haushalt"), CODE, List.of());

        assertThat(migrated.getMembers()).isEmpty();
    }

    @Test
    void memberListCannotBeChangedFromOutside() {
        Household household = Household.found(new HouseholdName("WG"), ALICE, CODE);

        assertThat(household.getMembers()).isUnmodifiable();
    }

    @Test
    void memberNameMustNotBeBlank() {
        assertThatIllegalArgumentException().isThrownBy(() -> new HouseholdMember(new UserId("id"), " "));
    }

    @Test
    void householdNameIsTrimmedAndLimited() {
        assertThat(new HouseholdName("  WG  ").value()).isEqualTo("WG");
        assertThatIllegalArgumentException().isThrownBy(() -> new HouseholdName("x".repeat(51)));
    }
}
