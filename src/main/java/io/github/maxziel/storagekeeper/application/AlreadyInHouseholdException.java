package io.github.maxziel.storagekeeper.application;

/**
 * Thrown when a user who already is a member of a household creates or joins another one.
 * A user is in at most one household.
 */
public class AlreadyInHouseholdException extends RuntimeException {
    public AlreadyInHouseholdException() {
        super("You are already a member of a household.");
    }
}
