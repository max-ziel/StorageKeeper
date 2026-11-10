package io.github.maxziel.storagekeeper.adapter.in.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * General information for the generated OpenAPI document ({@code /v3/api-docs}, Swagger UI under
 * {@code /swagger-ui.html}). Paths and schemas come from the controllers; this only adds the title
 * and declares that every endpoint needs a token, so Swagger UI offers an "Authorize" button with two ways:
 * logging in at Keycloak in the browser ({@value #KEYCLOAK}, authorization code with PKCE) or pasting a token
 * ({@value #BEARER_AUTH}).
 */
@Configuration
class OpenApiConfiguration {

    static final String BEARER_AUTH = "bearerAuth";
    static final String KEYCLOAK = "keycloak";

    @Bean
    OpenAPI storageKeeperOpenApi(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        // The browser talks to Keycloak directly, so these are the public URLs of the issuer.
        String openIdConnect = issuerUri + "/protocol/openid-connect";
        return new OpenAPI()
                .info(new Info()
                        .title("StorageKeeper API")
                        .description("Vorräte, Lagerorte und Einkaufsliste eines Haushalts verwalten.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(KEYCLOAK, new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                                        .authorizationUrl(openIdConnect + "/auth")
                                        .tokenUrl(openIdConnect + "/token"))))
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // Two separate requirements: either of them is enough.
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .addSecurityItem(new SecurityRequirement().addList(KEYCLOAK));
    }
}
