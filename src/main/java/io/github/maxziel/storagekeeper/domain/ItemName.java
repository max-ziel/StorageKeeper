package io.github.maxziel.storagekeeper.domain;

/**
 * Display name of a {@link PantryItem}, e.g. "Reis".
 *
 * <p>Invariants: trimmed, not blank, at most {@value #MAX_LENGTH} characters.
 */
public record ItemName(String value) {

    public static final int MAX_LENGTH = 100;

    public ItemName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw  new IllegalArgumentException("name must be at most" + MAX_LENGTH + " characters");
        }
    }
}
