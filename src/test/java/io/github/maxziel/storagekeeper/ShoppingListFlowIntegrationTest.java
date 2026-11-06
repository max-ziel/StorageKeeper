package io.github.maxziel.storagekeeper;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end check of the event flow: REST call, domain event, listener, shopping list in PostgreSQL.
 * Only a test with the whole application running shows that all parts are wired up correctly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ShoppingListFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void itemBelowMinimumAppearsOnTheListAndDisappearsWhenRestocked() throws Exception {
        String itemId = addItem("""
                {"name": "Kaffee", "amount": 1, "unit": "PACKAGE", "minimumAmount": 2}
                """);

        mockMvc.perform(get("/api/shopping-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].pantryItemId", hasItem(itemId)));

        mockMvc.perform(patch("/api/pantry-items/" + itemId + "/quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 3}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/shopping-list"))
                .andExpect(jsonPath("$[*].pantryItemId", not(hasItem(itemId))));
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
                .andExpect(jsonPath("$[?(@.pantryItemId == '" + itemId + "')].missingAmount").value(3));
    }

    @Test
    void deletingAnItemRemovesItsEntry() throws Exception {
        String itemId = addItem("""
                {"name": "Zucker", "amount": 0, "unit": "KILOGRAM", "minimumAmount": 1}
                """);

        mockMvc.perform(delete("/api/pantry-items/" + itemId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/shopping-list"))
                .andExpect(jsonPath("$[*].pantryItemId", not(hasItem(itemId))));
    }

    private String addItem(String json) throws Exception {
        String response = mockMvc.perform(post("/api/pantry-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}