package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.ShoppingListEntryNotFoundException;
import io.github.maxziel.storagekeeper.application.port.in.*;
import io.github.maxziel.storagekeeper.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShoppingListController.class)
@Import(SecurityConfiguration.class)
@WithMockUser
class ShoppingListControllerTest {

    private static final HouseholdId HOUSEHOLD = HouseholdId.newId();
    private static final StorageLocationId LOCATION = StorageLocationId.newId();
    private static final Instant NOW = Instant.parse("2027-02-26T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ListShoppingListUseCase listShoppingList;
    @MockitoBean
    private CheckOffStockPurchaseUseCase checkOffStockPurchase;
    @MockitoBean
    private AddManualShoppingListEntryUseCase addManualEntry;
    @MockitoBean
    private RemoveManualShoppingListEntryUseCase removeManualEntry;

    private final PantryItem rice = PantryItem.create(HOUSEHOLD, new ItemName("Reis"),
            new Quantity(new BigDecimal("0.5"), Unit.KILOGRAM), null, LOCATION);

    @Test
    void listShowsBothSections() throws Exception {
        rice.changeMinimumAmount(new BigDecimal("2"));
        ShoppingListEntry entry = ShoppingListEntry.create(rice.getPantryItemId(), NOW);
        ManualShoppingListEntry candles = ManualShoppingListEntry.create(HOUSEHOLD, new ItemName("Kerzen"), null, NOW);
        when(listShoppingList.getShoppingList())
                .thenReturn(new ShoppingList(List.of(new ShoppingListItem(entry, rice)), List.of(candles)));

        mockMvc.perform(get("/api/shopping-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockPurchases[0].id").value(entry.getShoppingListEntryId().toString()))
                .andExpect(jsonPath("$.stockPurchases[0].pantryItemId").value(rice.getPantryItemId().toString()))
                .andExpect(jsonPath("$.stockPurchases[0].name").value("Reis"))
                .andExpect(jsonPath("$.stockPurchases[0].unit").value("KILOGRAM"))
                .andExpect(jsonPath("$.stockPurchases[0].missingAmount").value(1.5))
                .andExpect(jsonPath("$.stockPurchases[0].addedAt").value("2027-02-26T10:00:00Z"))
                .andExpect(jsonPath("$.manualEntries[0].id").value(candles.getManualShoppingListEntryId().toString()))
                .andExpect(jsonPath("$.manualEntries[0].name").value("Kerzen"))
                .andExpect(jsonPath("$.manualEntries[0].amount").doesNotExist());
    }

    @Test
    void checkOffReturnsTheRestockedItem() throws Exception {
        ShoppingListEntryId entryId = ShoppingListEntryId.newId();
        rice.restock(BigDecimal.ONE);
        when(checkOffStockPurchase.checkOff(entryId, BigDecimal.ONE)).thenReturn(rice);

        mockMvc.perform(post("/api/shopping-list/stock-purchases/" + entryId + "/check-off")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rice.getPantryItemId().toString()))
                .andExpect(jsonPath("$.amount").value(1.5));
    }

    @Test
    void checkOffRejectsZeroAmount() throws Exception {
        mockMvc.perform(post("/api/shopping-list/stock-purchases/" + ShoppingListEntryId.newId() + "/check-off")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 0}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(checkOffStockPurchase);
    }

    @Test
    void checkOffOfUnknownEntryReturns404() throws Exception {
        ShoppingListEntryId entryId = ShoppingListEntryId.newId();
        when(checkOffStockPurchase.checkOff(any(), any())).thenThrow(new ShoppingListEntryNotFoundException(entryId));

        mockMvc.perform(post("/api/shopping-list/stock-purchases/" + entryId + "/check-off")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 1}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void addManualEntryReturnsLocationHeader() throws Exception {
        ManualShoppingListEntry candles = ManualShoppingListEntry.create(HOUSEHOLD, new ItemName("Kerzen"), null, NOW);
        when(addManualEntry.add(any(), isNull())).thenReturn(candles);

        mockMvc.perform(post("/api/shopping-list/manual-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Kerzen"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/shopping-list/manual-entries/" + candles.getManualShoppingListEntryId()));
    }
    
    @Test
    void addManualEntryWithAmount() throws Exception {
        Quantity twelve = new Quantity(new BigDecimal("12"), Unit.PIECE);
        ManualShoppingListEntry candles = ManualShoppingListEntry.create(HOUSEHOLD, new ItemName("Kerzen"), twelve, NOW);
        when(addManualEntry.add(new ItemName("Kerzen"), twelve)).thenReturn(candles);

        mockMvc.perform(post("/api/shopping-list/manual-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Kerzen", "amount": 12, "unit": "PIECE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(candles.getManualShoppingListEntryId().toString()))
                .andExpect(jsonPath("$.amount").value(12))
                .andExpect(jsonPath("$.unit").value("PIECE"));
    }

    @Test
    void addManualEntryWithoutAmount() throws Exception {
        ManualShoppingListEntry candles = ManualShoppingListEntry.create(HOUSEHOLD, new ItemName("Kerzen"), null, NOW);
        when(addManualEntry.add(any(), isNull())).thenReturn(candles);

        mockMvc.perform(post("/api/shopping-list/manual-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Kerzen"}
                                """))
                .andExpect(status().isCreated());

        verify(addManualEntry).add(new ItemName("Kerzen"), null);
    }

    @Test
    void addManualEntryRejectsAmountWithoutUnit() throws Exception {
        mockMvc.perform(post("/api/shopping-list/manual-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Kerzen", "amount": 12}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(addManualEntry);
    }

    @Test
    void addManualEntryRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/shopping-list/manual-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeManualEntryReturns204() throws Exception {
        ManualShoppingListEntryId id = ManualShoppingListEntryId.newId();

        mockMvc.perform(delete("/api/shopping-list/manual-entries/" + id))
                .andExpect(status().isNoContent());

        verify(removeManualEntry).remove(id);
    }

    @Test
    void removeUnknownManualEntryReturns404() throws Exception {
        ManualShoppingListEntryId id = ManualShoppingListEntryId.newId();
        doThrow(new ShoppingListEntryNotFoundException(id)).when(removeManualEntry).remove(id);

        mockMvc.perform(delete("/api/shopping-list/manual-entries/" + id))
                .andExpect(status().isNotFound());
    }
}