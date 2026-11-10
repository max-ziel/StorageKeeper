package io.github.maxziel.storagekeeper.adapter.out.persistence;

import io.github.maxziel.storagekeeper.domain.Household;
import io.github.maxziel.storagekeeper.domain.HouseholdId;
import io.github.maxziel.storagekeeper.domain.HouseholdMember;
import io.github.maxziel.storagekeeper.domain.HouseholdName;
import io.github.maxziel.storagekeeper.domain.InviteCode;
import io.github.maxziel.storagekeeper.domain.UserId;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Database row of table {@code household} with its members from {@code household_member}.
 * The members are part of the household aggregate, so they are stored and loaded with it.
 */
@Entity
@Table(name = "household")
class HouseholdJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = HouseholdName.MAX_LENGTH)
    private String name;

    @Column(name = "invite_code", nullable = false, unique = true, length = InviteCode.LENGTH)
    private String inviteCode;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "household_member", joinColumns = @JoinColumn(name = "household_id"))
    @OrderColumn(name = "position")
    private List<Member> members = new ArrayList<>();

    /** Required by JPA. */
    protected HouseholdJpaEntity() {
    }

    static HouseholdJpaEntity fromDomain(Household household) {
        HouseholdJpaEntity entity = new HouseholdJpaEntity();
        entity.id = household.getHouseholdId().value();
        entity.name = household.getName().value();
        entity.inviteCode = household.getInviteCode().value();
        entity.members = household.getMembers().stream()
                .map(member -> new Member(member.userId().value(), member.name()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        return entity;
    }

    Household toDomain() {
        return new Household(
                new HouseholdId(id),
                new HouseholdName(name),
                new InviteCode(inviteCode),
                members.stream().map(member -> new HouseholdMember(new UserId(member.userId), member.name)).toList());
    }

    /** One row of {@code household_member}. */
    @Embeddable
    static class Member {

        @Column(name = "user_id", nullable = false)
        private String userId;

        @Column(nullable = false)
        private String name;

        /** Required by JPA. */
        protected Member() {
        }

        Member(String userId, String name) {
            this.userId = userId;
            this.name = name;
        }
    }
}
