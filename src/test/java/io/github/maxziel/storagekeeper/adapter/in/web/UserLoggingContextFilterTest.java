package io.github.maxziel.storagekeeper.adapter.in.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserLoggingContextFilterTest {

    private final UserLoggingContextFilter filter = new UserLoggingContextFilter();
    private final List<String> userIdsSeenByChain = new ArrayList<>();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void putsTheUserIdIntoTheLoggingContextWhileTheRequestRuns() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            "sub-1234", null, AuthorityUtils.NO_AUTHORITIES));

        runFilter();

        assertThat(userIdsSeenByChain).containsExactly("sub-1234");
    }

    @Test
    void removesTheUserIdAfterTheRequest() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            "sub-1234", null, AuthorityUtils.NO_AUTHORITIES));

        runFilter();

        assertThat(MDC.get(UserLoggingContextFilter.USER_ID)).isNull();
    }

    @Test
    void removesTheUserIdEvenIfTheRequestFails() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            "sub-1234", null, AuthorityUtils.NO_AUTHORITIES));

        try {
            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (_, _) -> {
                throw new IllegalStateException("boom");
            });
        } catch (Exception expected) {
            // the filter must not swallow the exception
        }

        assertThat(MDC.get(UserLoggingContextFilter.USER_ID)).isNull();
    }

    @Test
    void addsNothingWithoutAuthentication() throws Exception {
        runFilter();

        assertThat(userIdsSeenByChain).containsExactly((String) null);
    }

    @Test
    void addsNothingForAnonymousRequests() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
            "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        runFilter();

        assertThat(userIdsSeenByChain).containsExactly((String) null);
    }

    private void runFilter() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
            (_, _) -> userIdsSeenByChain.add(MDC.get(UserLoggingContextFilter.USER_ID)));
    }
}
