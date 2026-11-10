package io.github.maxziel.storagekeeper.application.port.out;

/**
 * Tells the application who sent the current request. The application needs this to decide which household's
 * data a request may see, but it does not care how the user was authenticated (JWT, test user, ...).
 */
public interface CurrentUserProvider {

    /**
     * @throws IllegalStateException if there is no authenticated user, which would be a configuration error:
     *                               the web layer only lets authenticated requests through to the use cases
     */
    CurrentUser currentUser();
}
