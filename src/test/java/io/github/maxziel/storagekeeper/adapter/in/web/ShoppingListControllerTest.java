package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.port.in.ListShoppingListUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ShoppingListItem;
import io.github.maxziel.storagekeeper.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShoppingListController.class)
class ShoppingListControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ListShoppingListUseCase listShoppingList;

    @Test
    void listShowsMissingAmount() throws Exception {
        PantryItem rice = PantryItem.create(new ItemName("Reis"), new Quantity(new BigDecimal("0.5"), Unit.KILOGRAM),
                null, StorageLocationId.DEFAULT);
        rice.changeMinimumAmount(new BigDecimal("2"));
        ShoppingListEntry entry = ShoppingListEntry.create(rice.getPantryItemId(), Instant.parse("2027-02-26T10:00:00Z"));
        when(listShoppingList.listEntries()).thenReturn(List.of(new ShoppingListItem(entry, rice)));

        mockMvc.perform(get("/api/shopping-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(entry.getShoppingListEntryId().toString()))
                .andExpect(jsonPath("$[0].pantryItemId").value(rice.getPantryItemId().toString()))
                .andExpect(jsonPath("$[0].name").value("Reis"))
                .andExpect(jsonPath("$[0].unit").value("KILOGRAM"))
                .andExpect(jsonPath("$[0].missingAmount").value(1.5))
                .andExpect(jsonPath("$[0].addedAt").value("2027-02-26T10:00:00Z"));
    }
}