package io.github.maxziel.storagekeeper.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A group of users who share one pantry: its items, storage locations and shopping list.
 * Entity and aggregate root. Other aggregates refer to it only by {@link HouseholdId}.
 *
 * <p>Invariants: no user is listed twice. That a user belongs to at most one household is a rule across
 * households; it is checked by {@code HouseholdService} and secured by the database.
 */
public final class Household {

    private final HouseholdId householdId;
    private final HouseholdName name;
    private final InviteCode inviteCode;
    private final List<HouseholdMember> members;

    /**
     * Restores a household, e.g. when loading it from storage. Use {@link #found} for new households.
     * The member list may be empty for the household that the migration created for data from before M5.
     */
    public Household(HouseholdId householdId, HouseholdName name, InviteCode inviteCode,
                     List<HouseholdMember> members) {
        this.householdId = Objects.requireNonNull(householdId, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.inviteCode = Objects.requireNonNull(inviteCode, "invite code must not be null");
        this.members = new ArrayList<>();
        Objects.requireNonNull(members, "members must not be null").forEach(this::join);
    }

    /** Creates a new household with a fresh id; the founder is its first member. */
    public static Household found(HouseholdName name, HouseholdMember founder, InviteCode inviteCode) {
        return new Household(HouseholdId.newId(), name, inviteCode, List.of(founder));
    }

    /**
     * Adds a member.
     *
     * @throws IllegalStateException if the user is already a member
     */
    public void join(HouseholdMember member) {
        Objects.requireNonNull(member, "member must not be null");
        if (hasMember(member.userId())) {
            throw new IllegalStateException("user " + member.userId() + " is already a member");
        }
        members.add(member);
    }

    public boolean hasMember(UserId userId) {
        return members.stream().anyMatch(member -> member.userId().equals(userId));
    }

    public HouseholdId getHouseholdId() {
        return householdId;
    }

    public HouseholdName getName() {
        return name;
    }

    public InviteCode getInviteCode() {
        return inviteCode;
    }

    /** The members in the order they joined; unmodifiable. */
    public List<HouseholdMember> getMembers() {
        return List.copyOf(members);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Household other && householdId.equals(other.householdId);
    }

    @Override
    public int hashCode() {
        return householdId.hashCode();
    }
}
