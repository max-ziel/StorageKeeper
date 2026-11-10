package io.github.maxziel.storagekeeper.adapter.out.security;

import io.github.maxziel.storagekeeper.application.port.out.CurrentUser;
import io.github.maxziel.storagekeeper.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class SpringSecurityCurrentUserProviderTest {

    private final SpringSecurityCurrentUserProvider provider = new SpringSecurityCurrentUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtUserHasSubjectAsIdAndUserNameAsDisplayName() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("4f1c-sub")
            .claim(SpringSecurityCurrentUserProvider.DISPLAY_NAME_CLAIM, "alice")
            .build();
        // With authorities the token counts as authenticated, as after the real JWT check.
        SecurityContextHolder.getContext().setAuthentication(
            new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES));

        CurrentUser user = provider.currentUser();

        assertThat(user.userId()).isEqualTo(new UserId("4f1c-sub"));
        assertThat(user.displayName()).isEqualTo("alice");
    }

    @Test
    void withoutAuthenticationThereIsNoCurrentUser() {
        assertThatIllegalStateException().isThrownBy(provider::currentUser);
    }

    @Test
    void anonymousUserIsNoCurrentUser() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
            "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThatIllegalStateException().isThrownBy(provider::currentUser);
    }
}
