package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.PantryItemNotFoundException;
import io.github.maxziel.storagekeeper.application.StorageLocationNotFoundException;
import io.github.maxziel.storagekeeper.application.port.PageQuery;
import io.github.maxziel.storagekeeper.application.port.ResultPage;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PantryItemController.class)
@Import(SecurityConfiguration.class)
@WithMockUser
class PantryItemControllerTest {

    private static final HouseholdId HOUSEHOLD = HouseholdId.newId();
    private static final StorageLocationId LOCATION = StorageLocationId.newId();

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AddPantryItemUseCase addPantryItem;
    @MockitoBean
    private ListPantryItemUseCase listPantryItems;
    @MockitoBean
    private ChangeQuantityUseCase changeQuantity;
    @MockitoBean
    private RemovePantryItemUseCase removePantryItem;
    @MockitoBean
    private ListExpiringPantryItemsUseCase listExpiringPantryItems;
    @MockitoBean
    private ChangeMinimumAmountUseCase changeMinimumAmount;

    @Test
    void addReturnsCreatedItemWithLocation() throws Exception {
        PantryItem rice = PantryItem.create(HOUSEHOLD,
                new ItemName("Reis"),
                new Quantity(BigDecimal.ONE, Unit.KILOGRAM),
                LocalDate.of(2027, 3, 1),
                LOCATION
        );

        when(addPantryItem.add(any())).thenReturn(rice);

        mockMvc.perform(post("/api/pantry-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Reis", "amount": 1, "unit": "KILOGRAM", "expiryDate": "2027-03-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/pantry-items/" + rice.getPantryItemId()))
                .andExpect(jsonPath("$.name").value("Reis"))
                .andExpect(jsonPath("$.unit").value("KILOGRAM"))
                .andExpect(jsonPath("$.expiryDate").value("2027-03-01"))
                .andExpect(jsonPath("$.storageLocationId").value(LOCATION.toString()));
    }

    @Test
    void addRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/pantry-items")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name": "", "amount": -1, "unit": "KILOGRAM"}
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    @Test
    void addWithUnknownLocationReturnsBadRequest() throws Exception {
        StorageLocationId unknown = StorageLocationId.newId();
        when(addPantryItem.add(any())).thenThrow(new StorageLocationNotFoundException(unknown));

        mockMvc.perform(post("/api/pantry-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Reis", "amount": 1, "unit": "KILOGRAM", "storageLocationId": "%s"}
                                """.formatted(unknown)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Storage location " + unknown + " not found"));
    }

    @Test
    void removeOfUnknownItemReturnsNotFound() throws Exception {
        PantryItemId unknown = PantryItemId.newId();
        doThrow(new PantryItemNotFoundException(unknown)).when(removePantryItem).remove(unknown);

        mockMvc.perform(delete("/api/pantry-items/" + unknown))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Pantry item " + unknown + " not found"));
    }

    @Test
    void listExpiringReturnsDaysUntilExpiry() throws Exception {
        PantryItem milk = PantryItem.create(HOUSEHOLD, new ItemName("Milch"), new Quantity(BigDecimal.ONE, Unit.LITER),
                LocalDate.of(2027, 2, 24), LOCATION);
        when(listExpiringPantryItems.listExpiringWithin(3)).thenReturn(List.of(new ExpiringPantryItem(milk, -2)));

        mockMvc.perform(get("/api/pantry-items/expiring").param("days", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Milch"))
                .andExpect(jsonPath("$[0].expiryDate").value("2027-02-24"))
                .andExpect(jsonPath("$[0].daysUntilExpiry").value(-2))
                .andExpect(jsonPath("$[0].expired").value(true));
    }

    @Test
    void listExpiringContainsTheMinimumStock() throws Exception {
        PantryItem milk = PantryItem.create(HOUSEHOLD, new ItemName("Milch"), new Quantity(BigDecimal.ONE, Unit.LITER),
                LocalDate.of(2027, 2, 28), LOCATION);
        milk.changeMinimumAmount(new BigDecimal("2"));
        when(listExpiringPantryItems.listExpiringWithin(7)).thenReturn(List.of(new ExpiringPantryItem(milk, 2)));

        mockMvc.perform(get("/api/pantry-items/expiring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].minimumAmount").value(2))
                .andExpect(jsonPath("$[0].belowMinimum").value(true));
    }

    @Test
    void listExpiringUsesSevenDaysByDefault() throws Exception {
        when(listExpiringPantryItems.listExpiringWithin(7)).thenReturn(List.of());

        mockMvc.perform(get("/api/pantry-items/expiring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listExpiringRejectsDaysOutOfRange() throws Exception {
        mockMvc.perform(get("/api/pantry-items/expiring").param("days", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/pantry-items/expiring").param("days", "366"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listReturnsPageWithFiltersApplied() throws Exception {
        PantryItem rice = PantryItem.create(HOUSEHOLD, new ItemName("Reis"), new Quantity(BigDecimal.ONE, Unit.KILOGRAM), null,
                LOCATION);
        rice.changeMinimumAmount(new BigDecimal("2"));
        PantryItemFilter filter = new PantryItemFilter(LOCATION, true);
        when(listPantryItems.search(filter, new PageQuery(0, 5)))
                .thenReturn(new ResultPage<>(List.of(rice), 0, 5, 6));

        mockMvc.perform(get("/api/pantry-items")
                        .param("storageLocationId", LOCATION.toString())
                        .param("belowMinimum", "true")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Reis"))
                .andExpect(jsonPath("$.content[0].minimumAmount").value(2))
                .andExpect(jsonPath("$.content[0].belowMinimum").value(true))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void listWithoutParametersUsesFirstPageOfTwentyWithoutFilter() throws Exception {
        when(listPantryItems.search(PantryItemFilter.none(), new PageQuery(0, 20)))
                .thenReturn(new ResultPage<>(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/pantry-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void listRejectsInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/pantry-items").param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/pantry-items").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeMinimumAmountReturnsUpdatedItem() throws Exception {
        PantryItem rice = PantryItem.create(HOUSEHOLD, new ItemName("Reis"), new Quantity(BigDecimal.ONE, Unit.KILOGRAM), null,
                LOCATION);
        rice.changeMinimumAmount(new BigDecimal("0.5"));
        when(changeMinimumAmount.changeMinimumAmount(rice.getPantryItemId(), new BigDecimal("0.5"))).thenReturn(rice);

        mockMvc.perform(patch("/api/pantry-items/" + rice.getPantryItemId() + "/minimum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 0.5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimumAmount").value(0.5))
                .andExpect(jsonPath("$.belowMinimum").value(false));
    }
}
