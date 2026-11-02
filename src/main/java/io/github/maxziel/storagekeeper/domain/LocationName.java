package io.github.maxziel.storagekeeper.domain;

/**
 * Display name of a {@link StorageLocation}, e.g. "Kühlschrank".
 *
 * <p>Invariants: trimmed, not blank, at most {@value #MAX_LENGTH} characters.
 */
public record LocationName(String value) {
    public static final int MAX_LENGTH = 50;

    public LocationName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("location name must not be blank");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("location name must be at most " + MAX_LENGTH + " characters");
        }
    }
}
