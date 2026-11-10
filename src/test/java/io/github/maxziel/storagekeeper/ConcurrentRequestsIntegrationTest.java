package io.github.maxziel.storagekeeper;

import io.github.maxziel.storagekeeper.application.port.out.HouseholdRepository;
import io.github.maxziel.storagekeeper.application.port.out.StorageLocationRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Two requests that pass the service's check at the same time and then both write. The database constraint
 * rejects the second one; the client must get {@code 409}, not {@code 500}.
 *
 * <p>The race is forced, not hoped for: a spy on the repository lets each request wait after its check until the
 * other one has checked too. Only then do both continue and insert.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ConcurrentRequestsIntegrationTest {

    private static final String CONFLICT_DETAIL =
        "The request conflicts with data that was changed at the same time. Please reload and try again.";

    @Autowired
    private MockMvc mockMvc;
    @MockitoSpyBean
    private StorageLocationRepository storageLocationRepository;
    @MockitoSpyBean
    private HouseholdRepository householdRepository;

    private static JwtRequestPostProcessor user(String name) {
        String id = UUID.randomUUID().toString();
        return jwt().jwt(token -> token.subject(id).claim("preferred_username", name));
    }

    @Test
    void twoLocationsWithTheSameNameAtTheSameTime() throws Exception {
        JwtRequestPostProcessor alice = user("alice");
        createHousehold(alice);
        CyclicBarrier bothChecked = new CyclicBarrier(2);
        doAnswer(invocation -> {
            Object exists = invocation.callRealMethod();
            bothChecked.await(10, TimeUnit.SECONDS);
            return exists;
        }).when(storageLocationRepository).existsByName(any(), any());

        List<Integer> statuses = inParallel(() -> mockMvc.perform(post("/api/storage-locations")
                .with(alice)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Keller"}
                    """))
            .andReturn().getResponse());

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
    }

    @Test
    void sameUserFoundsTwoHouseholdsAtTheSameTime() throws Exception {
        JwtRequestPostProcessor bob = user("bob");
        CyclicBarrier bothChecked = new CyclicBarrier(2);
        doAnswer(invocation -> {
            Object household = invocation.callRealMethod();
            bothChecked.await(10, TimeUnit.SECONDS);
            return household;
        }).when(householdRepository).findByMember(any());

        List<Integer> statuses = inParallel(() -> mockMvc.perform(post("/api/household")
                .with(bob)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "Bobs Haushalt"}
                    """))
            .andReturn().getResponse());

        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
    }

    /**
     * Sends the request twice at the same time; also checks that a 409 has the generic, safe message.
     */
    private List<Integer> inParallel(Callable<MockHttpServletResponse> request)
        throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<MockHttpServletResponse> first = executor.submit(request);
            Future<MockHttpServletResponse> second = executor.submit(request);
            List<MockHttpServletResponse> responses =
                List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
            for (MockHttpServletResponse response : responses) {
                if (response.getStatus() == 409) {
                    assertThat(response.getContentAsString()).contains(CONFLICT_DETAIL);
                }
            }
            return responses.stream().map(MockHttpServletResponse::getStatus).toList();
        }
    }

    private void createHousehold(JwtRequestPostProcessor user) throws Exception {
        mockMvc.perform(post("/api/household")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "WG"}
                    """))
            .andExpect(status().isCreated());
    }
}
