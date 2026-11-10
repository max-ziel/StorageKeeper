package io.github.maxziel.storagekeeper.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds the id of the logged-in user ({@code sub} of the JWT) to every log line written while the request runs.
 * In structured logs it appears as the field {@code user.id} (ECS name), so all lines of one user can be found.
 * Runs in the security filter chain right after the token was checked.
 */
class UserLoggingContextFilter extends OncePerRequestFilter {

    static final String USER_ID = "user.id";

    @Override
    @SuppressWarnings("try") // the resource is only closed, never used inside the block
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        // Threads are reused for the next request, so the entry must be removed again in any case.
        try (MDC.MDCCloseable ignored = MDC.putCloseable(USER_ID, auth.getName())) {
            filterChain.doFilter(request, response);
        }
    }
}
