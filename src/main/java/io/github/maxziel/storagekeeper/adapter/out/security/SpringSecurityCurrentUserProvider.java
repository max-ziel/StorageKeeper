package io.github.maxziel.storagekeeper.adapter.out.security;

import io.github.maxziel.storagekeeper.application.port.out.CurrentUser;
import io.github.maxziel.storagekeeper.application.port.out.CurrentUserProvider;
import io.github.maxziel.storagekeeper.domain.UserId;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Implements {@link CurrentUserProvider} with the authentication Spring Security stored for the current request.
 * For a JWT the user id is the {@code sub} claim and the display name {@code preferred_username}
 * (Keycloak's user name); other authentications (e.g. test users) use their name for both.
 */
@Component
class SpringSecurityCurrentUserProvider implements CurrentUserProvider {

    static final String DISPLAY_NAME_CLAIM = "preferred_username";

    @Override
    public CurrentUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("no authenticated user");
        }

        // For a JwtAuthenticationToken, getName() is the "sub" claim.
        String displayName = authentication.getName();
        if (authentication instanceof JwtAuthenticationToken jwt
            && jwt.getToken().getClaimAsString(DISPLAY_NAME_CLAIM) != null) {
            displayName = jwt.getToken().getClaimAsString(DISPLAY_NAME_CLAIM);
        }
        return new CurrentUser(new UserId(authentication.getName()), displayName);
    }
}
