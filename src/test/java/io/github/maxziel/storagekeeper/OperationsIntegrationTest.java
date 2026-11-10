package io.github.maxziel.storagekeeper;

import com.jayway.jsonpath.JsonPath;
import io.github.maxziel.storagekeeper.domain.PantryItemId;
import io.github.maxziel.storagekeeper.domain.PantryItemReachedMinimum;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Runs the application with real HTTP ports, as in operation: the API on the main port and the actuator on the
 * separate management port. The JWT decoder is mocked, so no Keycloak is needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class OperationsIntegrationTest {

    private static final String TOKEN = "ops-test-token";

    @LocalServerPort
    private int serverPort;
    @LocalManagementPort
    private int managementPort;
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private RestClient api;
    private RestClient management;

    @BeforeEach
    void setUp() {
        api = client(serverPort);
        management = client(managementPort);
        when(jwtDecoder.decode(TOKEN)).thenReturn(Jwt.withTokenValue(TOKEN)
                .header("alg", "none")
                .subject("sub-operations-test")
                .claim("preferred_username", "olivia")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build());
    }

    @Test
    void managementPortIsSeparateFromTheMainPort() {
        assertThat(managementPort).isNotEqualTo(serverPort);
    }

    @Test
    void healthOnTheManagementPortShowsTheDatabaseWithoutToken() {
        ResponseEntity<String> response = management.get().uri("/actuator/health").retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read(response, "$.status")).isEqualTo("UP");
        assertThat(read(response, "$.components.db.status")).isEqualTo("UP");
    }

    @Test
    void readinessDependsOnTheDatabaseButLivenessDoesNot() {
        ResponseEntity<String> readiness = management.get().uri("/actuator/health/readiness")
                .retrieve().toEntity(String.class);
        ResponseEntity<String> liveness = management.get().uri("/actuator/health/liveness")
                .retrieve().toEntity(String.class);

        assertThat(read(readiness, "$.status")).isEqualTo("UP");
        assertThat(JsonPath.<Object>read(readiness.getBody(), "$.components")).asString().contains("db");
        assertThat(read(liveness, "$.status")).isEqualTo("UP");
        assertThat(liveness.getBody()).doesNotContain("\"db\"");
    }

    @Test
    void probesOnTheMainPortArePublic() {
        ResponseEntity<String> liveness = api.get().uri("/livez").retrieve().toEntity(String.class);
        ResponseEntity<String> readiness = api.get().uri("/readyz").retrieve().toEntity(String.class);

        assertThat(liveness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read(liveness, "$.status")).isEqualTo("UP");
        assertThat(readiness.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read(readiness, "$.status")).isEqualTo("UP");
        assertThat(read(readiness, "$.components.db.status")).isEqualTo("UP");
        assertThat(readiness.getBody()).doesNotContain("PostgreSQL");
    }

    @Test
    void metricsAreNotReachableOnTheMainPort() {
        ResponseEntity<String> response = api.get().uri("/actuator/prometheus").retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void infoShowsBuildAndJavaVersion() {
        ResponseEntity<String> response = management.get().uri("/actuator/info").retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read(response, "$.build.name")).isEqualTo("storagekeeper");
        assertThat(read(response, "$.build.version")).isNotBlank();
        assertThat(read(response, "$.java.version")).isNotBlank();
    }

    @Test
    void prometheusShowsRequestAndDomainEventMetrics() {
        callApi("/api/household", """
                {"name": "Betrieb"}
                """);
        String itemId = JsonPath.read(callApi("/api/pantry-items", """
                {"name": "Mehl", "amount": 3, "unit": "KILOGRAM", "minimumAmount": 2}
                """), "$.id");
        ResponseEntity<String> changed = api.patch().uri("/api/pantry-items/{id}/quantity", itemId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"amount": 1}
                        """)
                .retrieve().toEntity(String.class);
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);

        String metrics = management.get().uri("/actuator/prometheus").retrieve().body(String.class);

        assertThat(metrics)
                .contains("storagekeeper_domain_events_total{application=\"storagekeeper\","
                        + "type=\"PantryItemFellBelowMinimum\"} 1.0")
                .contains("http_server_requests_seconds_bucket")
                .contains("uri=\"/api/pantry-items/{id}/quantity\"");
    }

    @Test
    void eventsOfARolledBackTransactionAreNotCounted() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new PantryItemReachedMinimum(PantryItemId.newId()));
            status.setRollbackOnly();
        });

        assertThat(meterRegistry.find("storagekeeper.domain.events")
                .tag("type", "PantryItemReachedMinimum").counter()).isNull();
    }

    private String callApi(String path, String body) {
        ResponseEntity<String> response = api.post().uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().toEntity(String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private static String read(ResponseEntity<String> response, String jsonPath) {
        return JsonPath.read(response.getBody(), jsonPath);
    }

    /** Returns every response as it is; the tests check the status themselves. */
    private static RestClient client(int port) {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(_ -> true, (_, _) -> { })
                .build();
    }
}