package io.github.maxziel.storagekeeper.adapter.in.web;

import io.github.maxziel.storagekeeper.application.StorageLocationAlreadyExistsException;
import io.github.maxziel.storagekeeper.application.port.in.AddStorageLocationUseCase;
import io.github.maxziel.storagekeeper.application.port.in.ListStorageLocationsUseCase;
import io.github.maxziel.storagekeeper.domain.LocationName;
import io.github.maxziel.storagekeeper.domain.StorageLocation;
import io.github.maxziel.storagekeeper.domain.StorageLocationId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StorageLocationController.class)
class StorageLocationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AddStorageLocationUseCase addStorageLocation;
    @MockitoBean
    private ListStorageLocationsUseCase listStorageLocations;

    @Test
    void addReturnCreatedLocation() throws Exception {
        StorageLocation cellar = StorageLocation.create(new LocationName("Keller"));

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
                StorageLocation.create(new LocationName("Keller")),
                new StorageLocation(StorageLocationId.DEFAULT, new LocationName("Vorrat"))));

        mockMvc.perform(get("/api/storage-locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].defaultLocation").value(false))
                .andExpect(jsonPath("$[1].name").value("Vorrat"))
                .andExpect(jsonPath("$[1].defaultLocation").value(true));
    }
}
