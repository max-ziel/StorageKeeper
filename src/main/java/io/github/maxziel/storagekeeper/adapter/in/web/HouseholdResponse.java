package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.domain.Household;
import io.github.maxziel.storagekeeper.domain.HouseholdMember;

import java.util.List;
import java.util.UUID;

/**
 * JSON representation of a household. Only its members see it, so it includes the invite code
 * they can pass on. Members are listed by name in the order they joined; their user ids stay internal.
 */
public record HouseholdResponse(UUID id, String name, String inviteCode, List<String> members) {

    static HouseholdResponse from(Household household) {
        return new HouseholdResponse(
            household.getHouseholdId().value(),
            household.getName().value(),
            household.getInviteCode().value(),
            household.getMembers().stream().map(HouseholdMember::name).toList()
        );
    }
}
