package io.github.maxziel.storagekeeper.adapter.in.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * General information for the generated OpenAPI document ({@code /v3/api-docs}, Swagger UI under
 * {@code /swagger-ui.html}). Paths and schemas come from the controllers; this only adds the title
 * and declares that every endpoint needs a bearer token, so Swagger UI offers an "Authorize" button.
 */
@Configuration
class OpenApiConfiguration {

    static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI storageKeeperOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("StorageKeeper API")
                        .description("Vorräte, Lagerorte und Einkaufsliste eines Haushalts verwalten.")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}