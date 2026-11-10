package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.domain.HouseholdName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** JSON body for {@code POST /api/household}. */
public record CreateHouseholdRequest(@NotBlank @Size(max = HouseholdName.MAX_LENGTH) String name) {

    HouseholdName toHouseholdName() {
        return new HouseholdName(name);
    }
}
