package io.github.maxziel.storagekeeper.application.port.out;

import io.github.maxziel.storagekeeper.domain.UserId;

import java.util.Objects;

/**
 * The user who sent the current request.
 *
 * @param userId      stable id from the identity provider
 * @param displayName name to show to other members, e.g. "alice"
 */
public record CurrentUser(UserId userId, String displayName) {

    public CurrentUser {
        Objects.requireNonNull(userId, "user id must not be null");
        Objects.requireNonNull(displayName, "display name must not be null");
    }
}
