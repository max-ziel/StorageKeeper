package io.github.maxziel.storagekeeper.domain;

/**
 * Identity of a user. Users are managed by the identity provider (Keycloak), not by this application;
 * the value is the provider's stable user id (the {@code sub} claim of the token), not the user name,
 * which can change.
 *
 * <p>Invariants: not blank, at most {@value #MAX_LENGTH} characters.
 */
public record UserId(String value) {

    public static final int MAX_LENGTH = 255;

    public UserId {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("user id must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new DomainValidationException("user id must be at most " + MAX_LENGTH + " characters");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
