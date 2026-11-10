package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.NoHouseholdException;
import io.github.maxziel.storagekeeper.application.StorageLocationAlreadyExistsException;
import io.github.maxziel.storagekeeper.application.port.in.AddStorageLocationUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ListStorageLocationsUseCase;
import io.github.maxziel.storagekeeper.domain.DomainValidationException;
import io.github.maxziel.storagekeeper.domain.HouseholdId;
import io.github.maxziel.storagekeeper.domain.LocationName;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StorageLocationController.class)
@Import(SecurityConfiguration.class)
@WithMockUser
class StorageLocationControllerTest {

    private static final HouseholdId HOUSEHOLD = HouseholdId.newId();

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AddStorageLocationUseCase addStorageLocation;
    @MockitoBean
    private ListStorageLocationsUseCase listStorageLocations;

    @Test
    void addReturnCreatedLocation() throws Exception {
        StorageLocation cellar = StorageLocation.create(HOUSEHOLD, new LocationName("Keller"));

        when(addStorageLocation.add(any())).thenReturn(cellar);

        mockMvc.perform(post("/api/storage-locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Keller"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(cellar.getStorageLocationId().toString()))
                .andExpect(jsonPath("$.name").value("Keller"))
                .andExpect(jsonPath("$.defaultLocation").value(false));
    }

    @Test
    void addWithExistingNameReturnsConflict() throws Exception {
        when(addStorageLocation.add(any()))
                .thenThrow(new StorageLocationAlreadyExistsException(new LocationName("Keller")));

        mockMvc.perform(post("/api/storage-locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "keller"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void addRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/storage-locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listMarksDefaultLocation() throws Exception {
        when(listStorageLocations.listAll()).thenReturn(List.of(
                StorageLocation.create(HOUSEHOLD, new LocationName("Keller")),
                StorageLocation.createDefault(HOUSEHOLD)));

        mockMvc.perform(get("/api/storage-locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].defaultLocation").value(false))
                .andExpect(jsonPath("$[1].name").value("Vorrat"))
                .andExpect(jsonPath("$[1].defaultLocation").value(true));
    }

    @Test
    void userWithoutHouseholdGetsForbidden() throws Exception {
        when(listStorageLocations.listAll()).thenThrow(new NoHouseholdException());

        mockMvc.perform(get("/api/storage-locations"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Create or join a household first."));
    }

    @Test
    void addReturnsLocationHeader() throws Exception {
        StorageLocation cellar = StorageLocation.create(HOUSEHOLD, new LocationName("Keller"));
        when(addStorageLocation.add(any())).thenReturn(cellar);

        mockMvc.perform(post("/api/storage-locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Keller"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/storage-locations/" + cellar.getStorageLocationId()));
    }

    @Test
    void constraintViolationReturnsConflictWithoutDatabaseDetails() throws Exception {
        when(addStorageLocation.add(any())).thenThrow(new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uq_storage_location_household_name\""));

        mockMvc.perform(post("/api/storage-locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Keller"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(ApiExceptionHandler.CONFLICT_DETAIL));
    }

    @Test
    void domainRuleViolationReturnsBadRequestWithItsMessage() throws Exception {
        when(addStorageLocation.add(any())).thenThrow(new DomainValidationException("location name must not be blank"));

        mockMvc.perform(post("/api/storage-locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Keller"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("location name must not be blank"));
    }

    @Test
    void otherIllegalArgumentIsAnInternalErrorWithoutItsMessage() throws Exception {
        when(listStorageLocations.listAll()).thenThrow(new IllegalArgumentException("internal detail"));

        mockMvc.perform(get("/api/storage-locations"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(ApiExceptionHandler.UNEXPECTED_DETAIL));
    }

    @Test
    void illegalStateIsAnInternalErrorWithoutItsMessage() throws Exception {
        when(listStorageLocations.listAll()).thenThrow(new IllegalStateException("household has no default location"));

        mockMvc.perform(get("/api/storage-locations"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(ApiExceptionHandler.UNEXPECTED_DETAIL));
    }

    @Test
    void malformedJsonIsStillABadRequest() throws Exception {
        mockMvc.perform(post("/api/storage-locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void unsupportedMethodIsStillMethodNotAllowed() throws Exception {
        mockMvc.perform(delete("/api/storage-locations"))
                .andExpect(status().isMethodNotAllowed());
    }
}
