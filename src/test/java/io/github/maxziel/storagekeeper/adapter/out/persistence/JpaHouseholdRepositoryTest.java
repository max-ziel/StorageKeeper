package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.TestcontainersConfiguration;
import io.github.maxziel.storagekeeper.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Tag("integration")
@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaHouseholdRepository.class})
class JpaHouseholdRepositoryTest {

    private static final HouseholdMember ALICE = new HouseholdMember(new UserId("id-alice"), "alice");
    private static final HouseholdMember BOB = new HouseholdMember(new UserId("id-bob"), "bob");
    private static final HouseholdMember CAROL = new HouseholdMember(new UserId("id-carol"), "carol");

    @Autowired
    private JpaHouseholdRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savedHouseholdIsFoundByEachMemberWithMembersInJoinOrder() {
        Household household = Household.found(new HouseholdName("WG"), ALICE, InviteCode.generate());
        household.join(BOB);
        household.join(CAROL);
        repository.save(household);
        flushAndClear();

        Household loaded = repository.findByMember(BOB.userId()).orElseThrow();

        assertThat(loaded).isEqualTo(household);
        assertThat(loaded.getName()).isEqualTo(new HouseholdName("WG"));
        assertThat(loaded.getInviteCode()).isEqualTo(household.getInviteCode());
        assertThat(loaded.getMembers()).containsExactly(ALICE, BOB, CAROL);
        assertThat(repository.findByMember(ALICE.userId())).contains(household);
        assertThat(repository.findByMember(new UserId("id-dave"))).isEmpty();
    }

    @Test
    void joiningIsStoredWhenTheHouseholdIsSavedAgain() {
        Household household = Household.found(new HouseholdName("WG"), ALICE, InviteCode.generate());
        repository.save(household);
        flushAndClear();

        Household loaded = repository.findByInviteCode(household.getInviteCode()).orElseThrow();
        loaded.join(BOB);
        repository.save(loaded);
        flushAndClear();

        assertThat(repository.findByMember(BOB.userId()).orElseThrow().getMembers()).containsExactly(ALICE, BOB);
    }

    @Test
    void findByInviteCode() {
        Household household = Household.found(new HouseholdName("WG"), ALICE, InviteCode.generate());
        repository.save(household);
        flushAndClear();

        assertThat(repository.findByInviteCode(household.getInviteCode())).contains(household);
        assertThat(repository.findByInviteCode(new InviteCode("AAAA-BBBB"))).isEmpty();
    }

    @Test
    void databaseAllowsAUserInOneHouseholdOnly() {
        repository.save(Household.found(new HouseholdName("Erster"), ALICE, InviteCode.generate()));
        flushAndClear();

        assertThatExceptionOfType(PersistenceException.class).isThrownBy(() -> {
            repository.save(Household.found(new HouseholdName("Zweiter"), ALICE, InviteCode.generate()));
            entityManager.flush();
        });
    }

    @Test
    void inviteCodesAreUnique() {
        InviteCode code = InviteCode.generate();
        repository.save(Household.found(new HouseholdName("Erster"), ALICE, code));
        flushAndClear();

        assertThatExceptionOfType(PersistenceException.class).isThrownBy(() -> {
            repository.save(Household.found(new HouseholdName("Zweiter"), BOB, code));
            entityManager.flush();
        });
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
