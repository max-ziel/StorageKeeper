package io.github.maxziel.storagekeeper;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end check that every user only sees their own household's data, with JWTs as Keycloak issues them
 * ({@code sub} = user id, {@code preferred_username} = name) and the whole application against PostgreSQL.
 * Each test uses new users, so the tests do not depend on each other.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class HouseholdIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private JwtRequestPostProcessor alice;
    private JwtRequestPostProcessor bob;

    /**
     * A user as Keycloak would describe them in a token, with a new id for every test.
     */
    private static JwtRequestPostProcessor user(String name) {
        String id = UUID.randomUUID().toString();
        return jwt().jwt(token -> token.subject(id).claim("preferred_username", name));
    }

    @BeforeEach
    void setUp() {
        alice = user("alice");
        bob = user("bob");
    }

    @Test
    void userWithoutHouseholdSeesNoData() throws Exception {
        perform(get("/api/pantry-items"), alice)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.detail").value("Create or join a household first."));
        perform(get("/api/storage-locations"), alice).andExpect(status().isForbidden());
        perform(get("/api/shopping-list"), alice).andExpect(status().isForbidden());
        perform(get("/api/household"), alice).andExpect(status().isForbidden());
    }

    @Test
    void creatingAHouseholdMakesTheUserAMemberAndAddsTheDefaultLocation() throws Exception {
        createHousehold(alice, "WG Lindenstraße")
            .andExpect(jsonPath("$.name").value("WG Lindenstraße"))
            .andExpect(jsonPath("$.inviteCode").isString())
            .andExpect(jsonPath("$.members").value(contains("alice")));

        perform(get("/api/storage-locations"), alice)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Vorrat"))
            .andExpect(jsonPath("$[0].defaultLocation").value(true));
        perform(post("/api/household").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Zweiter\"}"), alice)
            .andExpect(status().isConflict());
    }

    @Test
    void householdsDoNotSeeEachOthersData() throws Exception {
        createHousehold(alice, "Alices Haushalt");
        createHousehold(bob, "Bobs Haushalt");
        String rice = addItem(alice, """
            {"name": "Reis", "amount": 1, "unit": "KILOGRAM", "expiryDate": "2027-01-01", "minimumAmount": 2}
            """);
        String candles = addManualEntry(alice);

        perform(get("/api/pantry-items"), bob)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
        perform(get("/api/pantry-items/expiring?days=365"), bob)
            .andExpect(jsonPath("$[*].id", not(hasItem(rice))));
        perform(get("/api/shopping-list"), bob)
            .andExpect(jsonPath("$.stockPurchases").isEmpty())
            .andExpect(jsonPath("$.manualEntries").isEmpty());
        perform(get("/api/storage-locations"), bob)
            .andExpect(jsonPath("$[*].name").value(contains("Vorrat")));

        // Bob cannot change or delete Alice's data even if he knows the ids; it looks as if they did not exist.
        perform(patch("/api/pantry-items/" + rice + "/quantity")
            .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 0}"), bob)
            .andExpect(status().isNotFound());
        perform(delete("/api/pantry-items/" + rice), bob).andExpect(status().isNotFound());
        perform(delete("/api/shopping-list/manual-entries/" + candles), bob).andExpect(status().isNotFound());
        perform(post("/api/shopping-list/stock-purchases/" + stockPurchaseEntryId(alice, rice) + "/check-off")
            .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 1}"), bob)
            .andExpect(status().isNotFound());

        // Alice's data is unchanged.
        perform(get("/api/pantry-items"), alice)
            .andExpect(jsonPath("$.content[0].id").value(rice))
            .andExpect(jsonPath("$.content[0].amount").value(1));
        perform(get("/api/shopping-list"), alice)
            .andExpect(jsonPath("$.manualEntries[0].id").value(candles));
    }

    @Test
    void itemCannotBePutIntoALocationOfAnotherHousehold() throws Exception {
        createHousehold(alice, "Alices Haushalt");
        createHousehold(bob, "Bobs Haushalt");
        String bobsCellar = JsonPath.read(perform(post("/api/storage-locations")
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Keller\"}"), bob)
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.id");

        perform(post("/api/pantry-items").contentType(MediaType.APPLICATION_JSON).content("""
            {"name": "Wein", "amount": 6, "unit": "PIECE", "storageLocationId": "%s"}
            """.formatted(bobsCellar)), alice)
            .andExpect(status().isBadRequest());

        // Location names only have to be unique within a household.
        perform(post("/api/storage-locations")
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Keller\"}"), alice)
            .andExpect(status().isCreated());
    }

    @Test
    void joiningWithTheInviteCodeSharesThePantry() throws Exception {
        String inviteCode = JsonPath.read(createHousehold(alice, "WG")
            .andReturn().getResponse().getContentAsString(), "$.inviteCode");
        String rice = addItem(alice, """
            {"name": "Reis", "amount": 1, "unit": "KILOGRAM"}
            """);

        perform(post("/api/household/join").contentType(MediaType.APPLICATION_JSON)
            .content("{\"inviteCode\": \"" + inviteCode.toLowerCase() + "\"}"), bob)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.members").value(contains("alice", "bob")));

        perform(get("/api/pantry-items"), bob)
            .andExpect(jsonPath("$.content[0].id").value(rice));
        perform(patch("/api/pantry-items/" + rice + "/quantity")
            .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 3}"), bob)
            .andExpect(status().isOk());
        perform(get("/api/household"), alice)
            .andExpect(jsonPath("$.members").value(contains("alice", "bob")));
    }

    @Test
    void joiningIsRejectedForUnknownCodesAndForUsersWhoAlreadyHaveAHousehold() throws Exception {
        String inviteCode = JsonPath.read(createHousehold(alice, "WG")
            .andReturn().getResponse().getContentAsString(), "$.inviteCode");

        perform(post("/api/household/join").contentType(MediaType.APPLICATION_JSON)
            .content("{\"inviteCode\": \"AAAA-AAAA\"}"), bob)
            .andExpect(status().isBadRequest());

        createHousehold(bob, "Bobs Haushalt");
        perform(post("/api/household/join").contentType(MediaType.APPLICATION_JSON)
            .content("{\"inviteCode\": \"" + inviteCode + "\"}"), bob)
            .andExpect(status().isConflict());
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, JwtRequestPostProcessor user)
        throws Exception {
        return mockMvc.perform(request.with(user));
    }

    private ResultActions createHousehold(JwtRequestPostProcessor user, String name) throws Exception {
        return perform(post("/api/household")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": \"" + name + "\"}"), user)
            .andExpect(status().isCreated());
    }

    private String addItem(JwtRequestPostProcessor user, String json) throws Exception {
        String response = perform(post("/api/pantry-items").contentType(MediaType.APPLICATION_JSON).content(json), user)
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String addManualEntry(JwtRequestPostProcessor user) throws Exception {
        String response = perform(post("/api/shopping-list/manual-entries")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\": \"" + "Kerzen" + "\"}"), user)
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String stockPurchaseEntryId(JwtRequestPostProcessor user, String itemId) throws Exception {
        String response = perform(get("/api/shopping-list"), user).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(response, "$.stockPurchases[?(@.pantryItemId == '" + itemId + "')].id");
        return ids.getFirst();
    }
}
