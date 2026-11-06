package io.github.maxziel.storagekeeper.adapter.in.web;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * The REST API is an OAuth2 resource server: every request to {@code /api/**} needs a valid JWT
 * from the configured issuer ({@code spring.security.oauth2.resourceserver.jwt.*}).
 * The API documentation and the health check stay public.
 */
@Configuration
class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http,
                                    @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        return http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(problemDetailEntryPoint(exceptionResolver)))
                // Every request carries its own token: no session, and so no session cookie a CSRF attack could use.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    /**
     * Answers a missing or invalid token with {@code 401}. The standard-bearer entry point sets the status and the
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
}