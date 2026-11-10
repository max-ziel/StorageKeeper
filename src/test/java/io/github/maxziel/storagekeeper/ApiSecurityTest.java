package io.github.maxziel.storagekeeper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Checks the security rules with the real filter chain: the API needs a JWT, documentation and health check do not.
 * {@code jwt()} puts an already validated token into the request, so no Keycloak is needed here; for tokens sent
 * in the header, the decoder is mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void apiRequestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/storage-locations"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("A valid bearer token is required."));
    }

    @Test
    void invalidTokenIsRejectedWithTheReason() throws Exception {
        when(jwtDecoder.decode("broken-token")).thenThrow(new BadJwtException("Jwt expired at 2026-01-01T00:00:00Z"));

        mockMvc.perform(get("/api/storage-locations").header(HttpHeaders.AUTHORIZATION, "Bearer broken-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("error=\"invalid_token\"")))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Jwt expired at 2026-01-01T00:00:00Z"));
    }

    @Test
    void writeRequestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/storage-locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Dachboden"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    /** Also checks that the user comes from the token: id from "sub", name from "preferred_username". */
    @Test
    void apiRequestWithTokenIsAccepted() throws Exception {
        mockMvc.perform(post("/api/household")
                        .with(jwt().jwt(token -> token.subject("sub-api-security-test")
                                .claim("preferred_username", "alice")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "WG"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.members[0]").value("alice"));
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