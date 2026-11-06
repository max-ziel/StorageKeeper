package io.github.maxziel.storagekeeper.domain;

/**
 * Display name of a {@link Household}, e.g. "WG Lindenstraße".
 *
 * <p>Invariants: trimmed, not blank, at most {@value #MAX_LENGTH} characters.
 */
public record HouseholdName(String value) {

    public static final int MAX_LENGTH = 50;

    public HouseholdName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("household name must not be blank");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("household name must be at most " + MAX_LENGTH + " characters");
        }
    }
}
