package io.github.maxziel.storagekeeper.application;

/**
 * Thrown when no household has the invite code a user wants to join with.
 */
public class UnknownInviteCodeException extends RuntimeException {
    public UnknownInviteCodeException() {
        // The code itself is left out of the message; it is a secret of the household it belongs to.
        super("No household has this invite code.");
    }
}
