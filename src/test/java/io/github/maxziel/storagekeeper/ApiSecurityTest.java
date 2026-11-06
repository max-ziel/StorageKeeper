package io.github.maxziel.storagekeeper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the security rules with the real filter chain: the API needs a JWT, documentation and health check do not.
 * {@code jwt()} puts an already validated token into the request, so no Keycloak is needed here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiRequestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/storage-locations"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));
    }

    @Test
    void writeRequestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/storage-locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Dachboden"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiRequestWithTokenIsAccepted() throws Exception {
        mockMvc.perform(get("/api/storage-locations").with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void openApiDocumentIsPublicAndDeclaresBearerAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("StorageKeeper API"))
                .andExpect(jsonPath("$.paths['/api/pantry-items']").exists())
                .andExpect(jsonPath("$.paths['/api/shopping-list']").exists())
                .andExpect(jsonPath("$.paths['/api/storage-locations']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists());
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}