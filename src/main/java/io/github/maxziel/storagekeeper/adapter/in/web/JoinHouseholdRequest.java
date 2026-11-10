package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.domain.InviteCode;
import jakarta.validation.constraints.NotBlank;

/**
 * JSON body for {@code POST /api/household/join}. The format of the code is checked by {@link InviteCode}
 * (a wrong format is a {@code 400} like every other invalid value).
 */
public record JoinHouseholdRequest(@NotBlank String inviteCode) {

    InviteCode toInviteCode() {
        return new InviteCode(inviteCode);
    }
}
