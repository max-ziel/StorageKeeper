package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.PantryItemNotFoundException;
import io.github.maxziel.storagekeeper.application.port.in.AddPantryItemUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ChangeQuantityUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ListPantryItemUseCase;
import io.github.maxziel.storagekeeper.application.port.in.RemovePantryItemUseCase;
import io.github.maxziel.storagekeeper.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PantryItemController.class)
class PantryItemControllerTest {

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

    @Test
    void addReturnsCreatedItemWithLocation() throws  Exception {
        PantryItem rice = PantryItem.create(
                new ItemName("Reis"),
                new Quantity(BigDecimal.ONE, Unit.KILOGRAM),
                LocalDate.of(2027,3,1)
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
                .andExpect(jsonPath("$.expiryDate").value("2027-03-01"));
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
    void removeOfUnknownItemReturnsNotFound() throws Exception {
        PantryItemId unknown = PantryItemId.newId();
        doThrow(new PantryItemNotFoundException(unknown)).when(removePantryItem).remove(unknown);

        mockMvc.perform(delete("/api/pantry-items/" + unknown))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Pantry item " + unknown + " not found"));
    }
}
