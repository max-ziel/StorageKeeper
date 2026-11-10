package io.github.maxziel.storagekeeper;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The realm files for development and deployment must offer the browser client that Swagger UI is configured with,
 * otherwise "Authorize" fails with "Client not found". Reads the files as Keycloak imports them; no Keycloak needed.
 */
class KeycloakRealmFilesTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static JsonNode realm(String file) {
        // Gradle runs the tests in the project directory.
        return JSON.readTree(Path.of(file).toFile());
    }

    private static JsonNode client(JsonNode realm, String clientId) {
        return StreamSupport.stream(realm.get("clients").spliterator(), false)
            .filter(client -> client.get("clientId").asString().equals(clientId))
            .findFirst()
            .orElseThrow(() -> new AssertionError("client " + clientId + " missing"));
    }

    private static Properties applicationConfig() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        return yaml.getObject();
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev/keycloak/storagekeeper-realm.json", "deploy/keycloak/storagekeeper-realm.json"})
    void realmOffersTheSwaggerUiClientWithPkce(String realmFile) {
        Properties config = applicationConfig();
        JsonNode client = client(realm(realmFile), config.getProperty("springdoc.swagger-ui.oauth.client-id"));

        assertThat(client.get("publicClient").asBoolean()).isTrue();
        assertThat(client.get("standardFlowEnabled").asBoolean()).isTrue();
        assertThat(client.get("directAccessGrantsEnabled").asBoolean()).isFalse();
        assertThat(client.get("attributes").get("pkce.code.challenge.method").asString()).isEqualTo("S256");
        assertThat(client.get("redirectUris").get(0).asString()).endsWith("/swagger-ui/oauth2-redirect.html");
        assertThat(client.get("protocolMappers").get(0).get("config").get("included.custom.audience").asString())
            .isEqualTo(config.getProperty("spring.security.oauth2.resourceserver.jwt.audiences"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"deploy/keycloak/storagekeeper-realm.json"})
    void productionRealmHasNoUsersAndNoPasswordGrant(String realmFile) {
        JsonNode realm = realm(realmFile);

        assertThat(realm.has("users")).isFalse();
        assertThat(realm.get("clients").values())
            .allSatisfy(client -> assertThat(client.get("directAccessGrantsEnabled").asBoolean()).isFalse());
    }
}
