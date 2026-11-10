package io.github.maxziel.storagekeeper.adapter.in.web;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * The REST API is an OAuth2 resource server: every request to {@code /api/**} needs a valid JWT
 * from the configured issuer ({@code spring.security.oauth2.resourceserver.jwt.*}).
 * The API documentation and the health probes stay public; the actuator endpoints run on their own port,
 * see {@link ManagementSecurity}.
 */
@Configuration
class SecurityConfiguration {

    /**
     * Answers a missing or invalid token with {@code 401}. The standard bearer token entry point sets the status and the
     * {@code WWW-Authenticate} header (RFC 6750); the body is written by {@link ApiExceptionHandler}, so a 401 is a
     * {@code ProblemDetail} like every other error. The filter runs before Spring MVC, so the exception is handed
     * to MVC's exception resolver explicitly.
     */
    private static AuthenticationEntryPoint problemDetailEntryPoint(HandlerExceptionResolver exceptionResolver) {
        AuthenticationEntryPoint bearerEntryPoint = new BearerTokenAuthenticationEntryPoint();
        return (request, response, exception) -> {
            bearerEntryPoint.commence(request, response, exception);
            exceptionResolver.resolveException(request, response, null, exception);
        };
    }

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http,
                                    @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        return http
            .authorizeHttpRequests(requests -> requests
                // Health probes on the main port (management.endpoint.health.probes.add-additional-paths).
                .requestMatchers("/livez", "/readyz").permitAll()
                // Only used if the management port is set to the main port: then health and info stay
                // public, and metrics need a token like the API.
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(Customizer.withDefaults())
                .authenticationEntryPoint(problemDetailEntryPoint(exceptionResolver)))
            // Not a @Component: Spring Boot would register it a second time as a filter outside the chain.
            .addFilterAfter(new UserLoggingContextFilter(), BearerTokenAuthenticationFilter.class)
            // Every request carries its own token: no session, and so no session cookie a CSRF attack could use.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .build();
    }

    /**
     * The actuator endpoints on the separate management port ({@code management.server.port}) are open without a
     * token, so that Prometheus and health checks need no credentials. This is safe only because that port is not
     * reachable from outside; it must never be published. With a different management port, {@link EndpointRequest}
     * matches only requests on that port, so {@code /actuator/**} on the main port is not affected.
     * If both ports are the same, this chain does not exist and {@link #apiSecurity} applies to the actuator too.
     */
    @Configuration
    @ConditionalOnManagementPort(ManagementPortType.DIFFERENT)
    static class ManagementSecurity {

        @Bean
        @Order(1)
        SecurityFilterChain managementSecurity(HttpSecurity http) {
            return http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .build();
        }
    }
}
