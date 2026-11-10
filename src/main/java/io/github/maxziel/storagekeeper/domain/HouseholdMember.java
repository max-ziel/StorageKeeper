package io.github.maxziel.storagekeeper.domain;

import java.util.Objects;

/**
 * A user as a member of a {@link Household}. The name is only for display ("who else is in my household")
 * and is taken from the identity provider when the user joins.
 *
 * <p>Invariants: user id set; name trimmed, not blank, at most {@value #MAX_NAME_LENGTH} characters.
 */
public record HouseholdMember(UserId userId, String name) {

    public static final int MAX_NAME_LENGTH = 255;

    public HouseholdMember {
        Objects.requireNonNull(userId, "user id must not be null");
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("member name must not be blank");
        }
        name = name.strip();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new DomainValidationException("member name must be at most " + MAX_NAME_LENGTH + " characters");
        }
    }
}
