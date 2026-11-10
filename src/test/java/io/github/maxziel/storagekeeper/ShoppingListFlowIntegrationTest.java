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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end check of the event flow: REST call, domain event, listener, shopping list in PostgreSQL.
 * Only a test with the whole application running shows that all parts are wired up correctly.
 * All tests run as the same user, who needs a household first.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@WithMockUser
class ShoppingListFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /** The first test creates the household; for the others it already exists (409). */
    @BeforeEach
    void ensureHousehold() throws Exception {
        mockMvc.perform(post("/api/household")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Testhaushalt"}
                                """))
                .andExpect(status().is(anyOf(is(201), is(409))));
    }

    @Test
    void itemBelowMinimumAppearsOnTheListAndDisappearsWhenRestocked() throws Exception {
        String itemId = addItem("""
                {"name": "Kaffee", "amount": 1, "unit": "PACKAGE", "minimumAmount": 2}
                """);

        mockMvc.perform(get("/api/shopping-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockPurchases[*].pantryItemId", hasItem(itemId)));

        mockMvc.perform(patch("/api/pantry-items/" + itemId + "/quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 3}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/shopping-list"))
                .andExpect(jsonPath("$.stockPurchases[*].pantryItemId", not(hasItem(itemId))));
    }

    @Test
    void raisingTheMinimumPutsTheItemOnTheList() throws Exception {
        String itemId = addItem("""
                {"name": "Tee", "amount": 1, "unit": "PACKAGE"}
                """);

        mockMvc.perform(patch("/api/pantry-items/" + itemId + "/minimum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 4}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/shopping-list"))
                .andExpect(jsonPath("$.stockPurchases[?(@.pantryItemId == '" + itemId + "')].missingAmount")
                        .value(3));
    }

    @Test
    void deletingAnItemRemovesItsEntry() throws Exception {
        String itemId = addItem("""
                {"name": "Zucker", "amount": 0, "unit": "KILOGRAM", "minimumAmount": 1}
                """);

        mockMvc.perform(delete("/api/pantry-items/" + itemId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/shopping-list"))
                .andExpect(jsonPath("$.stockPurchases[*].pantryItemId", not(hasItem(itemId))));
    }

    @Test
    void partialCheckOffKeepsTheEntryWithTheRemainingAmount() throws Exception {
        String itemId = addItem("""
                {"name": "Mehl", "amount": 0, "unit": "KILOGRAM", "minimumAmount": 3}
                """);
        String entryId = stockPurchaseEntryId(itemId);

        checkOff(entryId, "1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1))
                .andExpect(jsonPath("$.belowMinimum").value(true));

        mockMvc.perform(get("/api/shopping-list"))
                .andExpect(jsonPath("$.stockPurchases[?(@.id == '" + entryId + "')].missingAmount").value(2));
    }

    @Test
    void fullCheckOffRemovesTheEntry() throws Exception {
        String itemId = addItem("""
                {"name": "Nudeln", "amount": 1, "unit": "PACKAGE", "minimumAmount": 3}
                """);
        String entryId = stockPurchaseEntryId(itemId);

        checkOff(entryId, "2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(3))
                .andExpect(jsonPath("$.belowMinimum").value(false));

        mockMvc.perform(get("/api/shopping-list"))
                .andExpect(jsonPath("$.stockPurchases[*].id", not(hasItem(entryId))));
        checkOff(entryId, "1").andExpect(status().isNotFound());
    }

    @Test
    void manualEntriesAreListedSeparatelyAndRemovedWhenCheckedOff() throws Exception {
        String response = mockMvc.perform(post("/api/shopping-list/manual-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Geburtstagskerzen", "amount": 12, "unit": "PIECE"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String entryId = JsonPath.read(response, "$.id");

        mockMvc.perform(get("/api/shopping-list"))
                .andExpect(jsonPath("$.manualEntries[?(@.id == '" + entryId + "')].name")
                        .value(contains("Geburtstagskerzen")))
                .andExpect(jsonPath("$.stockPurchases[*].id", not(hasItem(entryId))));

        mockMvc.perform(delete("/api/shopping-list/manual-entries/" + entryId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/shopping-list"))
                .andExpect(jsonPath("$.manualEntries[*].id", not(hasItem(entryId))));
    }

    private String addItem(String json) throws Exception {
        String response = mockMvc.perform(post("/api/pantry-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String stockPurchaseEntryId(String itemId) throws Exception {
        String response = mockMvc.perform(get("/api/shopping-list"))
                .andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(response, "$.stockPurchases[?(@.pantryItemId == '" + itemId + "')].id");
        return ids.getFirst();
    }

    private ResultActions checkOff(String entryId, String amount) throws Exception {
        return mockMvc.perform(post("/api/shopping-list/stock-purchases/" + entryId + "/check-off")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": " + amount + "}"));
    }
}