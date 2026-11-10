package io.github.maxziel.storagekeeper.application;

/**
 * Thrown when a user who is not a member of any household uses a pantry or shopping list use case.
 * All data belongs to a household, so there is nothing such a user may see or change.
 */
public class NoHouseholdException extends RuntimeException {
    public NoHouseholdException() {
        super("Create or join a household first.");
    }
}
