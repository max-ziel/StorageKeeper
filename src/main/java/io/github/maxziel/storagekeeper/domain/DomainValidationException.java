package io.github.maxziel.storagekeeper.domain;

/**
 * A value breaks a rule of the domain, e.g. a blank name or a negative amount. The message describes the broken
 * rule in a way that can be shown to the user, so it must never contain internal details.
 *
 * <p>Extends {@link IllegalArgumentException}, because that is what it is; the own type lets the web adapter
 * answer exactly these with {@code 400} and treat any other {@code IllegalArgumentException} as a programming
 * error ({@code 500}).
 */
public class DomainValidationException extends IllegalArgumentException {
    public DomainValidationException(String message) {
        super(message);
    }
}
